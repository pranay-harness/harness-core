package software.wings.core.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.UUIDGenerator;
import software.wings.utils.Misc;
import software.wings.utils.ThreadContext;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by peeyushaggarwal on 4/13/16.
 *
 * @param <T> the generic type
 * @see Queuable
 */
public abstract class AbstractQueueListener<T extends Queuable> implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(AbstractQueueListener.class);

  @Inject private Queue<T> queue;

  private boolean runOnce;

  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  @Inject @Named("timer") private ScheduledExecutorService timer;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    String threadName = ThreadContext.getContext() + queue.name() + "-handler-" + UUIDGenerator.getUuid();
    logger.debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    boolean run = !runOnce;
    do {
      T message = null;
      try {
        logger.trace("Waiting for message");
        message = queue.get();
        logger.trace("got message {}", message);
      } catch (Exception exception) {
        if (exception instanceof InterruptedException
            || exception.getCause() != null
                && exception.getCause().getClass().isAssignableFrom(InterruptedException.class)) {
          Misc.info(logger, "Thread interrupted, shutting down for queue " + queue.name(), exception);
          run = false;
        } else {
          Misc.error(logger, "Exception happened while fetching message from queue " + queue.name(), exception);
        }
      }

      if (message != null) {
        long timerInterval = queue.resetDurationMillis() - 500;
        logger.debug("Started timer thread for message {} every {} ms", message, timerInterval);
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

  /**
   * On message.
   *
   * @param message the message
   * @throws Exception the exception
   */
  protected abstract void onMessage(T message) throws Exception;

  /**
   * On exception.
   *
   * @param exception the exception
   * @param message   the message
   */
  protected void onException(Exception exception, T message) {
    Misc.error(logger, "Exception happened while processing message " + message, exception);
    if (message.getRetries() > 0) {
      message.setRetries(message.getRetries() - 1);
      queue.requeue(message);
    }
  }

  /**
   * Gets queue.
   *
   * @return the queue
   */
  public Queue<T> getQueue() {
    return queue;
  }

  /**
   * Sets queue.
   *
   * @param queue the queue
   */
  void setQueue(Queue<T> queue) {
    this.queue = queue;
  }

  /**
   * Shut down.
   */
  public void shutDown() {
    shouldStop.set(true);
  }

  /**
   * Sets run once.
   *
   * @param runOnce the run once
   */
  // Package protected for testing
  void setRunOnce(boolean runOnce) {
    this.runOnce = runOnce;
  }

  /**
   * Sets timer.
   *
   * @param timer the timer
   */
  void setTimer(ScheduledExecutorService timer) {
    this.timer = timer;
  }
}
