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

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestMain {

    private static final AtomicBoolean throttlingReq = new AtomicBoolean(false);
    private static final ScheduledExecutorService throttlingExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService bulkInsertExecutor = Executors.newScheduledThreadPool(5);

    public static void main(String[] args) throws InterruptedException {

        MongoClient mongoClient = getMongoClent();

        CheckThrottling checkThrottling = new CheckThrottling(mongoClient, throttlingExecutor, throttlingReq);
        checkThrottling.check(); // check whether we need throttling or not

        BulkInsertMongo bulkInsertMongo = new BulkInsertMongo(mongoClient, bulkInsertExecutor, throttlingReq);
        int[] arr = new int[20];
        Arrays.fill(arr, 1);
        bulkInsertMongo.insertVarLoad(arr, 2000, true);
        System.out.println("-----------------------------------DONE----------------------------------");
        Thread.sleep(1000 * 60);
        bulkInsertMongo.insertVarLoad(new int[]{10, 100, 100, 100, 1000, 1000, 100, 100, 100, 10}, 100, false);
        System.out.println("-----------------------------------DONE----------------------------------");
        Thread.sleep(1000 * 60);
        arr = new int[100];
        Arrays.fill(arr, 1);
        bulkInsertMongo.insertVarLoad(arr, 5000, true);
        System.out.println("-----------------------------------DONE----------------------------------");
        Thread.sleep(1000 * 60);
        bulkInsertMongo.insertVarLoad(arr, 10000, true);
        System.out.println("-----------------------------------DONE----------------------------------");
    }

    private static MongoClient getMongoClent() {
        return new MongoClient("localhost", 27017);
    }
}
