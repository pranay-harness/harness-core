package io.harness.threading;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ForceQueuePolicy based on https://github.com/AndroidDeveloperLB/ListViewVariants
 * /blob/master/app/src/main
 * /java/lb/listviewvariants/utils/async_task_thread_pool/ForceQueuePolicy.java used in the
 * threadpool executor that forces the Java to raise the current pool size, if it has still not
 * reached the max threshold, in case existing ones are busy processing other jobs.
 */
@Slf4j
public class ForceQueuePolicy implements RejectedExecutionHandler {
  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    try {
      logger.info("rejectedExecution occurred - will force the thread pool to increase pool size");
      executor.getQueue().put(r);
    } catch (InterruptedException ex) {
      // should never happen since we never wait
      throw new RejectedExecutionException(ex);
    }
  }
}
