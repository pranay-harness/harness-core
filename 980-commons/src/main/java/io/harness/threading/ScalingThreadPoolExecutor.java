/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.threading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * ThreadPoolExecutor based on https://github.com/AndroidDeveloperLB/ListViewVariants
 * /blob/master/app/src/main /java/lb/listviewvariants/utils/async_task_thread_pool
 * /ScalingThreadPoolExecutor.java that forces the Java to raise the current pool size, if it has
 * still not reached the max threshold, in case existing ones are busy processing other jobs.
 */
@Slf4j
public class ScalingThreadPoolExecutor extends ThreadPoolExecutor {
  /**
   * number of threads that are actively executing tasks
   */
  private final AtomicInteger activeCount = new AtomicInteger();

  /**
   * Instantiates a new scaling thread pool executor.
   */
  public ScalingThreadPoolExecutor() {
    super(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));
  }

  /**
   * Instantiates a new scaling thread pool executor.
   *
   * @param corePoolSize    the core pool size
   * @param maximumPoolSize the maximum pool size
   * @param keepAliveTime   the keep alive time
   * @param unit            the unit
   * @param workQueue       the work queue
   */
  public ScalingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue workQueue, ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
  }

  @Override
  public Future<?> submit(Runnable task) {
    log.debug("Task submitted: {}", task);
    return super.submit(task);
  }

  @Override
  public int getActiveCount() {
    return activeCount.get();
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ThreadPoolExecutor#beforeExecute(java.lang.Thread, java.lang.Runnable)
   */
  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    activeCount.incrementAndGet();
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable, java.lang.Throwable)
   */
  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    activeCount.decrementAndGet();
    if (t != null) {
      log.error("Unhandled Exception: ", t);
    }
  }
}
