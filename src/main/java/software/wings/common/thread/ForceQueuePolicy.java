package software.wings.common.thread;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  ForceQueuePolicy based on
 * https://github.com/AndroidDeveloperLB/ListViewVariants/blob/master/app/src/main/java/lb/listviewvariants/utils/async_task_thread_pool/ForceQueuePolicy.java
 *  used in the threadpool executor that forces the Java to raise the current pool size, if it has still not reached the
 * max threshold, in case existing ones are busy processing other jobs.
 *
 *
 * @author Rishi
 *
 */
public class ForceQueuePolicy implements RejectedExecutionHandler {
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    try {
      logger.debug("rejectedExecution occured - will force the threadpool to icrease pool size");
      executor.getQueue().put(r);
    } catch (InterruptedException e) {
      // should never happen since we never wait
      throw new RejectedExecutionException(e);
    }
  }
  private static Logger logger = LoggerFactory.getLogger(ForceQueuePolicy.class);
}