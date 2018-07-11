package software.wings.core.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.exception.WingsException;
import software.wings.utils.ThreadContext;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
  @Inject private ConfigurationController configurationController;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    String threadName = ThreadContext.getContext() + queue.name() + "-handler-" + generateUuid();
    logger.debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    do {
      while (isMaintenance() || configurationController.isNotPrimary()) {
        sleep(Duration.ofSeconds(1));
      }

      if (!execute()) {
        break;
      }

    } while (!runOnce && !shouldStop.get());
  }

  public boolean execute() {
    T message = null;
    try {
      logger.trace("Waiting for message");
      message = queue.get();
    } catch (Exception exception) {
      if (exception.getCause() instanceof InterruptedException) {
        logger.info("Thread interrupted, shutting down for queue {}", queue.name());
        return false;
      }
      logger.error("Exception happened while fetching message from queue {}", queue.name(), exception);
    }

    if (message == null) {
      return true;
    }

    if (logger.isTraceEnabled()) {
      logger.trace("got message {}", message);
    }

    long timerInterval = queue.resetDurationMillis() - 500;
    if (logger.isDebugEnabled()) {
      logger.debug("Started timer thread for message {} every {} ms", message, timerInterval);
    }
    final T finalizedMessage = message;
    ScheduledFuture<?> future = timer.scheduleAtFixedRate(
        () -> queue.updateResetDuration(finalizedMessage), timerInterval, timerInterval, TimeUnit.MILLISECONDS);
    try {
      onMessage(message);
      queue.ack(message);
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      onException(exception, message);
    } finally {
      future.cancel(true);
    }
    return true;
  }

  /**
   * On message.
   *
   * @param message the message
   */
  protected abstract void onMessage(T message);

  /**
   * On exception.
   *
   * @param exception the exception
   * @param message   the message
   */
  void onException(Exception exception, T message) {
    logger.error("Exception happened while processing message " + message, exception);
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

  void setConfigurationController(ConfigurationController configurationController) {
    this.configurationController = configurationController;
  }
}
