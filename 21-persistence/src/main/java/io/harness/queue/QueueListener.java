package io.harness.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.govern.Switch.noop;
import static io.harness.maintenance.MaintenanceController.isMaintenance;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class QueueListener<T extends Queuable> implements Runnable {
  @Inject @Getter @Setter private Queue<T> queue;

  @Setter private boolean runOnce;
  private final boolean primaryOnly;

  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  @Inject private TimerScheduledExecutorService timer;
  @Inject private QueueController queueController;

  public QueueListener(boolean primaryOnly) {
    this.primaryOnly = primaryOnly;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    String threadName = queue.name() + "-handler-" + generateUuid();
    logger.debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    do {
      while (isMaintenance() || (primaryOnly && queueController.isNotPrimary())) {
        sleep(ofSeconds(1));
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
      message = queue.get(ofSeconds(3), ofSeconds(1));
    } catch (Exception exception) {
      if (exception.getCause() instanceof InterruptedException) {
        logger.info("Thread interrupted, shutting down for queue {}", queue.name());
        return false;
      }
      logger.error(format("Exception happened while fetching message from queue %s", queue.name()), exception);
    }

    if (message != null) {
      processMessage(message);
    }
    return true;
  }

  public void pumpAll() {
    while (true) {
      T message = null;
      try {
        logger.trace("Waiting for message");
        message = queue.get(Duration.ZERO, Duration.ZERO);
      } catch (Exception exception) {
        if (exception.getCause() instanceof InterruptedException) {
          logger.info("Thread interrupted, shutting down for queue {}", queue.name());
          return;
        }
        logger.error(format("Exception happened while fetching message from queue %s", queue.name()), exception);
      }

      if (message == null) {
        break;
      }
      processMessage(message);
    }
  }

  @SuppressWarnings("PMD")
  private void processMessage(T message) {
    if (logger.isTraceEnabled()) {
      logger.trace("got message {}", message);
    }

    long timerInterval = queue.resetDurationMillis() - 500;
    if (logger.isDebugEnabled()) {
      logger.debug("Started timer thread for message {} every {} ms", message, timerInterval);
    }

    try {
      final T finalizedMessage = message;

      ScheduledFuture<?> future = timer.scheduleAtFixedRate(
          () -> queue.updateResetDuration(finalizedMessage), timerInterval, timerInterval, TimeUnit.MILLISECONDS);
      try {
        try (GlobalContextGuard guard = GlobalContextManager.initGlobalContextGuard(message.getGlobalContext())) {
          onMessage(message);
        } catch (IOException e) {
          throw new WingsException("Something failed in initializing GlobalContextGuard:  " + e);
        }
      } finally {
        future.cancel(true);
      }

      queue.ack(message);
    } catch (Throwable exception) {
      logger.error(format("Exception happened in onMessage %s", queue.name()), exception);
      if (exception instanceof InstantiationError) {
        // we can never process this message. Just purge it.
        queue.ack(message);
      } else if (exception instanceof Exception) {
        onException((Exception) exception, message);
      } else {
        // we have already logged Throwable exception above. no-op here.
        noop();
      }
    }
  }

  public abstract void onMessage(T message);

  protected void requeue(T message) {
    queue.requeue(message.getId(), message.getRetries() - 1);
  }

  /**
   * On exception.
   *
   * @param exception the exception
   * @param message   the message
   */
  public void onException(Exception exception, T message) {
    if (exception instanceof WingsException) {
      ExceptionLogger.logProcessedMessages((WingsException) exception, MANAGER, logger);
    } else {
      logger.error("Exception happened while processing message " + message, exception);
    }

    if (message.getRetries() > 0) {
      requeue(message);
    } else {
      logger.error("Out of retries for message " + message, exception);
      queue.ack(message);
    }
  }

  public void shutDown() {
    shouldStop.set(true);
  }
}
