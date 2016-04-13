package software.wings.core.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
public abstract class AbstractQueueListener<T extends Queuable> implements Runnable {
  @Inject private Queue<T> queue;

  private boolean runOnce;

  @Override
  public void run() {
    boolean run = !runOnce;
    do {
      T message = null;
      try {
        message = queue.get(Integer.MAX_VALUE);
      } catch (Exception e) {
        if (e.getCause() != null && e.getCause().getClass().isAssignableFrom(InterruptedException.class)) {
          log().info("Thread interrupted, shutting down for queue " + queue.getName() + e);
          run = false;
        }
        log().error("Exception happened while fetching message from queue " + queue.getName(), e);
      }
      if (message != null) {
        try {
          onMessage(message);
          queue.ack(message);
        } catch (Exception e) {
          onException(e, message);
        }
      }
    } while (run);
  }

  protected abstract void onMessage(T message) throws Exception;

  protected void onException(Exception e, T message) {
    log().error("Exception happened while processing message " + message, e);
    queue.requeue(message);
  }

  // Package protected for testing
  void setRunOnce(boolean runOnce) {
    this.runOnce = runOnce;
  }

  void setQueue(Queue<T> queue) {
    this.queue = queue;
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
