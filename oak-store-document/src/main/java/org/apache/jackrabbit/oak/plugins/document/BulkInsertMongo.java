/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
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
