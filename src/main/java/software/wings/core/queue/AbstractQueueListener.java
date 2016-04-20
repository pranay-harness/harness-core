package software.wings.core.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.UUIDGenerator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Named;

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
      } catch (Exception exception) {
        if (exception.getCause() != null
            && exception.getCause().getClass().isAssignableFrom(InterruptedException.class)) {
          log().info("Thread interrupted, shutting down for queue {}, Exception: " + exception, queue.name());
          run = false;
        } else {
          log().error("Exception happened while fetching message from queue " + queue.name(), exception);
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
        } catch (Exception exception) {
          onException(exception, message);
        } finally {
          future.cancel(true);
        }
      }
    } while (run && !shouldStop.get());
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }

  protected abstract void onMessage(T message) throws Exception;

  protected void onException(Exception exception, T message) {
    log().error("Exception happened while processing message " + message, exception);
    if (message.getRetries() > 0) {
      message.setRetries(message.getRetries() - 1);
      queue.requeue(message);
    }
  }

  public Queue<T> getQueue() {
    return queue;
  }

  void setQueue(Queue<T> queue) {
    this.queue = queue;
  }

  public void shutDown() {
    shouldStop.set(true);
  }

  // Package protected for testing
  void setRunOnce(boolean runOnce) {
    this.runOnce = runOnce;
  }

  void setTimer(ScheduledExecutorService timer) {
    this.timer = timer;
  }
}
