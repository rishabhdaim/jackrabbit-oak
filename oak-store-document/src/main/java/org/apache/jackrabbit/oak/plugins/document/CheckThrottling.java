/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.jackrabbit.oak.plugins.document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

public class CheckThrottling {

    static final String TS_TIME = "ts";
    static final String NATURAL = "$natural";
    static final String MAX_SIZE = "maxSize";
    static final String OPLOG_RS = "oplog.rs";
    private final ScheduledExecutorService throttlingExecutor;
    private final AtomicBoolean throttlingReq;
    private final MongoDatabase localDb;

    public CheckThrottling(final MongoClient mongoClient, ScheduledExecutorService throttlingExecutor, AtomicBoolean throttlingReq) {
        this.throttlingExecutor = throttlingExecutor;
        this.throttlingReq = throttlingReq;
        this.localDb = mongoClient.getDatabase("local");
    }

    public void check() {
        throttlingExecutor.scheduleAtFixedRate(() -> {
            final MongoIterable<String> strings = localDb.listCollectionNames();
            String ol = null;
            for (String e: strings) {
                if (Objects.equals(e, OPLOG_RS)) {
                    ol = OPLOG_RS;
                    break;
                }
            }
            if (Objects.isNull(ol)) {
                System.out.println("replication not detected");
            } else {
                Document document = localDb.runCommand(new Document("collStats", OPLOG_RS));
                if (!document.containsKey(MAX_SIZE)) {
                    System.out.println("Could not get stats for local." + ol + " collection. collstats returned: " + document);
                } else {
                    int maxSize = document.getInteger(MAX_SIZE);
                    double maxSizeMb = (double) maxSize / (1024 * 1024);
                    System.out.printf("Max Size : %.4f%n", maxSizeMb);
                    int usedSize = document.getInteger("size");
                    double usedSizeMb = Math.ceil(((double) usedSize / (1024 * 1024)) * 1000) / 1000;
                    System.out.printf("Used Size MB : %.4f%n", usedSizeMb);
                    MongoCollection<Document> localDbCollection = localDb.getCollection(ol);
                    Document first = localDbCollection.find().sort(new Document(NATURAL, 1)).limit(1).first();
                    Document last = localDbCollection.find().sort(new Document(NATURAL, -1)).limit(1).first();

                    if (Objects.isNull(first) || Objects.isNull(last)) {
                        System.out.println("objects not found in local.oplog.rs -- is this a new and empty db instance?");
                    } else {
                        if (first.containsKey(TS_TIME) && last.containsKey(TS_TIME)) {
                            BsonTimestamp startTime = first.get(TS_TIME, BsonTimestamp.class);
                            BsonTimestamp lastTime = last.get(TS_TIME, BsonTimestamp.class);
                            System.out.printf("First Oplog Entry : %d%n", startTime.getTime());
                            System.out.printf("Last Oplog Entry : %d%n", lastTime.getTime());
                            long timeDiffSec = Math.abs(lastTime.getTime() - startTime.getTime());
                            double timeDiffHr = Math.ceil(((double)timeDiffSec/(60*60)) * 100000)/100000;
                            System.out.printf("Oplog Entries Time Diff in Hour : %s%n", timeDiffHr);
                            double currentOplogHourRate = usedSizeMb/timeDiffHr;
                            System.out.printf("Oplog-Mb/Hour : %f%n", currentOplogHourRate);
                            double leftTime = maxSizeMb/currentOplogHourRate;
                            System.out.printf("Time left as per current Oplog/Hour : %f%n", leftTime);
                            if (leftTime < 0.5) {
                                System.err.println("Go for throttling");
                                throttlingReq.set(true);
                            } else {
                                System.err.println("Keep Rolling!!!!");
                                throttlingReq.set(false);
                            }
                        }
                    }
                }
            }
        }, 0, 10, SECONDS);
    }
}
