package software.wings.core.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.UUIDGenerator;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
public abstract class AbstractQueueListener<T extends Queuable> implements Runnable {
  @Inject private Queue<T> queue;

  private boolean runOnce;

  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  @Inject @Named("timer") private ScheduledExecutorService timer;

  @Override
  public void run() {
    String threadName = queue.name() + "-handler-" + UUIDGenerator.getUUID();
    log().debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    boolean run = !runOnce;
    do {
      T message = null;
      try {
        log().trace("Waiting for message");
        message = queue.get();
        log().trace("got message {}", message);
      } catch (Exception e) {
        if (e.getCause() != null && e.getCause().getClass().isAssignableFrom(InterruptedException.class)) {
          log().info("Thread interrupted, shutting down for queue {}, Exception: " + e, queue.name());
          run = false;
        } else {
          log().error("Exception happened while fetching message from queue " + queue.name(), e);
        }
      }

      if (message != null) {
        long timerInterval = queue.resetDurationMillis() - 500;
        log().debug("Started timer thread for message {} every {} ms", message, timerInterval);
        final T finalizedMessage = message;
        ScheduledFuture<?> future = timer.scheduleAtFixedRate(
            () -> queue.updateResetDuration(finalizedMessage), timerInterval, timerInterval, TimeUnit.MILLISECONDS);
        try {
          onMessage(message);
          queue.ack(message);
        } catch (Exception e) {
          onException(e, message);
        } finally {
          future.cancel(true);
        }
      }
    } while (run && !shouldStop.get());
  }

  protected abstract void onMessage(T message) throws Exception;

  protected void onException(Exception e, T message) {
    log().error("Exception happened while processing message " + message, e);
    if (message.getRetries() > 0) {
      message.setRetries(message.getRetries() - 1);
      queue.requeue(message);
    }
  }

  public Queue<T> getQueue() {
    return queue;
  }

  public void shutDown() {
    shouldStop.set(true);
  }
  // Package protected for testing
  void setRunOnce(boolean runOnce) {
    this.runOnce = runOnce;
  }

  void setQueue(Queue<T> queue) {
    this.queue = queue;
  }

  void setTimer(ScheduledExecutorService timer) {
    this.timer = timer;
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
