package io.harness.workers.background.critical.iterator;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.distribution.constraint.Consumer.State;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;
import software.wings.service.intfc.ResourceConstraintService;

@Slf4j
public class ResourceConstraintBackupHandler implements Handler<ResourceConstraintInstance> {
  private static final String HANDLER_NAME = "ResourceConstraint-Backup";
  private static final int POOL_SIZE = 10;
  private static final int SCHEDULE_INTERVAL_SECONDS = 60;

  @Inject private ResourceConstraintService resourceConstraintService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  public void registerIterators() {
    PumpExecutorOptions executorOptions = PumpExecutorOptions.builder()
                                              .name(HANDLER_NAME)
                                              .poolSize(POOL_SIZE)
                                              .interval(ofSeconds(SCHEDULE_INTERVAL_SECONDS))
                                              .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(executorOptions,
        ResourceConstraintBackupHandler.class,
        MongoPersistenceIterator.<ResourceConstraintInstance>builder()
            .clazz(ResourceConstraintInstance.class)
            .fieldName(ResourceConstraintInstanceKeys.nextIteration)
            .filterExpander(q -> q.filter(ResourceConstraintInstanceKeys.state, State.ACTIVE.name()))
            .targetInterval(ofSeconds(30))
            .acceptableNoAlertDelay(ofSeconds(30))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(ResourceConstraintInstance instance) {
    try {
      resourceConstraintService.updateActiveConstraintForInstance(instance);
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException e) {
      logger.error("", e);
    }
  }
}
