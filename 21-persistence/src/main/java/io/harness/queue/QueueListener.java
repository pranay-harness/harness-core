package io.harness.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFilename;
import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.mongo.DelayLogContext;
import io.harness.mongo.MessageLogContext;
import io.harness.mongo.ProcessTimeLogContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class QueueListener<T extends Queuable> implements Runnable {
  @Setter private boolean runOnce;
  private final boolean primaryOnly;

  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  @Inject private TimerScheduledExecutorService timer;
  @Inject private QueueController queueController;

  @Getter @Setter private QueueConsumer<T> queueConsumer;

  public QueueListener(QueueConsumer<T> queueConsumer, boolean primaryOnly) {
    this.queueConsumer = queueConsumer;
    this.primaryOnly = primaryOnly;
  }

  @Override
  public void run() {
    String threadName = queueConsumer.getName() + "-handler-" + generateUuid();
    logger.debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    do {
      while (getMaintenanceFilename() || (primaryOnly && queueController.isNotPrimary())) {
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
      message = queueConsumer.get(ofSeconds(3), ofSeconds(1));
    } catch (Exception exception) {
      if (exception.getCause() instanceof InterruptedException) {
        logger.info("Thread interrupted, shutting down for queue {}", queueConsumer.getName());
        return false;
      }
      logger.error("Exception happened while fetching message from queue {}", queueConsumer.getName(), exception);
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
        message = queueConsumer.get(Duration.ZERO, Duration.ZERO);
      } catch (Exception exception) {
        if (exception.getCause() instanceof InterruptedException) {
          logger.info("Thread interrupted, shutting down for queue {}", queueConsumer.getName());
          return;
        }
        logger.error("Exception happened while fetching message from queue {}", queueConsumer.getName(), exception);
      }

      if (message == null) {
        break;
      }
      processMessage(message);
    }
  }

  @SuppressWarnings({"PMD", "squid:S1181"})
  private void processMessage(T message) {
    long startTime = currentTimeMillis();

    try (MessageLogContext ignore = new MessageLogContext(message, OVERRIDE_ERROR)) {
      long timerInterval = queueConsumer.heartbeat().toMillis() - 500;
      final T finalizedMessage = message;
      ScheduledFuture<?> future = timer.scheduleAtFixedRate(
          () -> queueConsumer.updateHeartbeat(finalizedMessage), timerInterval, timerInterval, TimeUnit.MILLISECONDS);

      try (GlobalContextGuard guard = initGlobalContextGuard(message.getGlobalContext())) {
        long delay = startTime - message.getEarliestGet().toInstant().toEpochMilli();
        try (DelayLogContext ignore2 = new DelayLogContext(delay, OVERRIDE_ERROR)) {
          logger.info("Working on message");
        }

        onMessage(message);
      } finally {
        future.cancel(true);
      }

      queueConsumer.ack(message);
    } catch (InstantiationError exception) {
      logger.error("Critical exception happened in onMessage {}", queueConsumer.getName(), exception);
      queueConsumer.ack(message);
    } catch (Throwable exception) {
      onException(exception, message);
    } finally {
      long processTime = currentTimeMillis() - startTime;
      try (ProcessTimeLogContext ignore2 = new ProcessTimeLogContext(processTime, OVERRIDE_ERROR)) {
        logger.info("Done with message");
      } catch (Throwable exception) {
        logger.error("Exception while recording the processing of message", exception);
      }
    }
  }

  public abstract void onMessage(T message);

  protected void requeue(T message) {
    queueConsumer.requeue(message.getId(), message.getRetries() - 1);
  }

  public void onException(Throwable exception, T message) {
    if (exception instanceof WingsException) {
      ExceptionLogger.logProcessedMessages((WingsException) exception, MANAGER, logger);
    } else {
      logger.error("Exception happened while processing message " + message, exception);
    }

    if (message.getRetries() > 0) {
      requeue(message);
    } else {
      logger.error("Out of retries for message " + message, exception);
      queueConsumer.ack(message);
    }
  }

  public void shutDown() {
    shouldStop.set(true);
  }
}
