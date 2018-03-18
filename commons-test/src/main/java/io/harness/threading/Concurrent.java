package io.harness.threading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Concurrent {
  Concurrent() {}

  public static void test(int threadCount, Runnable runnable) throws InterruptedException {
    final CyclicBarrier barrier = new CyclicBarrier(threadCount);

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < threadCount; ++i) {
      final Thread thread = new Thread(() -> {
        try {
          barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
          // ignore
        }
        runnable.run();
      });
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }
}
