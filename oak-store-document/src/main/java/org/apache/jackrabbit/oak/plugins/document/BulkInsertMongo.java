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
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BulkInsertMongo {
    private final ScheduledExecutorService bulkInsertExecutor;
    private final AtomicBoolean throttlingReq;

    private static final Map<String, Object> rishu = Map.of("name", "rishu", "fullname", "rishabh kumar", "father name", "Sh. Rajesh Kumar", "age", 31, "city", "bhadson", "company", "adobe", "job_title", "software developer");
    private static final Map<String, Object> anku = Map.of("name", "anku", "fullname", "ankit kumar","father name", "Sh. Rajesh Kumar","age", 29, "city", "bhadson", "company", "KPMG", "job_title", "consultant");
    private static final Map<String, Object> munna = Map.of("name", "munna", "fullname", "umesh kumar","father name", "Sh. Jagdish Chand","age", 28, "city", "winnipeg", "company", "resturant", "job_title", "manager");
    private static final Map<String, Object> aru = Map.of("name", "aru", "fullname", "aryan verma","father name", "Sh. Ashwani Kumar","age", 19, "city", "mohali", "company", "SD College", "job_title", "student");
    private static final Map<String, Object> deep = Map.of("name", "deep", "fullname", "deepinder kumar daim","father name", "Sh. Yadwinder Kumar","age", 11, "city", "patiala", "company", "DAV Public School", "job_title", "student");
    private static final Map<String, Object> dev = Map.of("name", "dev", "fullname", "devansh rajesh daim","father name", "Sh. Rishabh Kumar","age", 1, "city", "bhadson", "company", "home", "job_title", "new born");
    private final MongoCollection<Document> testCollection;

    public BulkInsertMongo(MongoClient mongoClient, ScheduledExecutorService bulkInsertExecutor, AtomicBoolean throttlingReq) {

        this.bulkInsertExecutor = bulkInsertExecutor;
        this.throttlingReq = throttlingReq;
        final MongoDatabase aemAuthorDb = mongoClient.getDatabase("aem-author");
        this.testCollection = aemAuthorDb.getCollection("test");
    }

    public void insertVarLoad(final int[] varLoad, final long sleepTime, final boolean slow) throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        for (int j : varLoad) {
            if (slow) Thread.sleep(sleepTime);
            final int itr = count.addAndGet(1);
            bulkInsertExecutor.schedule(() -> {
                for (int i = 0; i < j; i++) {
                    long startTime = currentTimeMillis();
                    final boolean isThrottled = throttlingReq.get();
                    if (isThrottled) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    @SuppressWarnings("RedundantTypeArguments (explicit type arguments speedup compilation and analysis time)")
                    BulkWriteResult bulkWriteResult = testCollection.bulkWrite(slow ? List.<InsertOneModel<Document>>of(new InsertOneModel<>(new Document(rishu)),
                            new InsertOneModel<>(new Document(dev))) :
                    List.<InsertOneModel<Document>>of(new InsertOneModel<>(new Document(rishu)),
                            new InsertOneModel<>(new Document(rishu)),
                            new InsertOneModel<>(new Document(anku)),
                            new InsertOneModel<>(new Document(anku)),
                            new InsertOneModel<>(new Document(munna)),
                            new InsertOneModel<>(new Document(munna)),
                            new InsertOneModel<>(new Document(aru)),
                            new InsertOneModel<>(new Document(aru)),
                            new InsertOneModel<>(new Document(deep)),
                            new InsertOneModel<>(new Document(deep)),
                            new InsertOneModel<>(new Document(dev)),
                            new InsertOneModel<>(new Document(dev))));
                    if (isThrottled)
                        System.err.printf("Insert Count : %d in %d ms with system throttled in %d iteration %n", bulkWriteResult.getInsertedCount(), (currentTimeMillis() - startTime), itr);
                    else
                        System.out.printf("Insert Count : %d in %d ms without system throttled in %d iteration %n", bulkWriteResult.getInsertedCount(), (currentTimeMillis() - startTime), itr);

                }
            }, 1, SECONDS);
        }
    }
}
