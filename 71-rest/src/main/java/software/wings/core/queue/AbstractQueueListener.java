package software.wings.core.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.queue.Queuable;
import io.harness.queue.Queue;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.exception.WingsExceptionMapper;
import software.wings.utils.ThreadContext;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractQueueListener<T extends Queuable> implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(AbstractQueueListener.class);

  @Inject @Getter @Setter private Queue<T> queue;

  private @Setter boolean runOnce;
  private final boolean primaryOnly;

  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  @Inject @Named("timer") @Setter private ScheduledExecutorService timer;
  @Inject @Setter private ConfigurationController configurationController;

  public AbstractQueueListener(boolean primaryOnly) {
    this.primaryOnly = primaryOnly;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    String threadName = ThreadContext.getContext() + queue.name() + "-handler-" + generateUuid();
    logger.debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    do {
      while (isMaintenance() || (primaryOnly && configurationController.isNotPrimary())) {
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
      logger.error(format("Exception happened while fetching message from queue %s", queue.name()), exception);
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

    ScheduledFuture<?> future = null;
    try {
      final T finalizedMessage = message;
      future = timer.scheduleAtFixedRate(
          () -> queue.updateResetDuration(finalizedMessage), timerInterval, timerInterval, TimeUnit.MILLISECONDS);

      onMessage(message);
      queue.ack(message);
    } catch (Exception exception) {
      onException(exception, message);
    } finally {
      if (future != null) {
        future.cancel(true);
      }
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
    if (exception instanceof WingsException) {
      WingsExceptionMapper.logProcessedMessages((WingsException) exception, MANAGER, logger);
    } else {
      logger.error("Exception happened while processing message " + message, exception);
    }
    if (message.getRetries() > 0) {
      message.setRetries(message.getRetries() - 1);
      queue.requeue(message);
    }
  }

  public void shutDown() {
    shouldStop.set(true);
  }
}
