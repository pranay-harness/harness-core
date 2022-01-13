/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.threading;

import io.harness.exception.ConcurrentException;
import io.harness.exception.CyclicBarrierException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.function.IntConsumer;

public class Concurrent {
  Concurrent() {}

  public static void test(int threadCount, IntConsumer runnable) {
    final CyclicBarrier barrier = new CyclicBarrier(threadCount);

    final List<RuntimeException> exceptions = new ArrayList<>();

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < threadCount; ++i) {
      final int number = i;
      final Thread thread = new Thread(() -> {
        try {
          barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
          Thread.currentThread().interrupt();
          throw new CyclicBarrierException(e);
        }
        runnable.accept(number);
      });

      thread.setUncaughtExceptionHandler((t, e) -> exceptions.add((RuntimeException) e));
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ConcurrentException(e);
      }
    }

    if (!exceptions.isEmpty()) {
      // This will expose only one of the exceptions, but since this is used from test that needs to be fixed
      // when the first one is fixed it will come the time for the next
      throw exceptions.get(0);
    }
  }
}
