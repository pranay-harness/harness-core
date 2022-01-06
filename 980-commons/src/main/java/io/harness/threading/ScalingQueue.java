/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.threading;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ScalingQueue based on https://github.com/AndroidDeveloperLB/ListViewVariants/blob
 * /master/app/src/main /java/lb/listviewvariants/utils/async_task_thread_pool/ScalingQueue.java
 * used in the thread pool executor that forces the Java to raise the current pool size, if it has
 * still not reached the max threshold, in case existing ones are busy processing other jobs.
 *
 * @param <E> the element type
 */
public class ScalingQueue<E> extends LinkedBlockingQueue<E> {
  private static final long serialVersionUID = 2006711824734916827L;
  /**
   * The executor this Queue belongs to
   */
  private transient ThreadPoolExecutor executor;

  /**
   * Creates a TaskQueue with a capacity of {@link Integer#MAX_VALUE}.
   */
  public ScalingQueue() {}

  /**
   * Creates a TaskQueue with the given (fixed) capacity.
   *
   * @param capacity the capacity of this queue.
   */
  public ScalingQueue(int capacity) {
    super(capacity);
  }

  /**
   * Sets the executor this queue belongs to.
   *
   * @param executor the executor
   */
  public void setThreadPoolExecutor(ThreadPoolExecutor executor) {
    this.executor = executor;
  }

  /**
   * Inserts the specified element at the tail of this queue if there is at least one available
   * thread to run the current task. If all pool threads are actively busy, it rejects the offer.
   *
   * @param o the element to add.
   * @return true if it was possible to add the element to this queue, else false
   * @see ThreadPoolExecutor#execute(Runnable)
   */
  @Override
  public boolean offer(E o) {
    int allWorkingThreads = executor.getActiveCount() + super.size();
    return allWorkingThreads < executor.getPoolSize() && super.offer(o);
  }
}
