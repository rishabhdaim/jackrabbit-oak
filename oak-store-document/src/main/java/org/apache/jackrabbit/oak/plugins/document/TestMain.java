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
