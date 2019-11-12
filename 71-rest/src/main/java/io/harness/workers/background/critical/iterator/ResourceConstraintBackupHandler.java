package io.harness.workers.background.critical.iterator;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.ResourceConstraintInstance.NOT_FINISHED_STATES;

import com.google.common.collect.Sets;
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
  private static final String handlerName = "ResourceConstraint-Backup";
  private static final int poolSize = 10;
  private static final int scheduleIntervalSeconds = 60;

  @Inject private ResourceConstraintService resourceConstraintService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  public void registerIterators() {
    PumpExecutorOptions executorOptions = PumpExecutorOptions.builder()
                                              .name(handlerName)
                                              .poolSize(poolSize)
                                              .interval(ofSeconds(scheduleIntervalSeconds))
                                              .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(executorOptions,
        ResourceConstraintBackupHandler.class,
        MongoPersistenceIterator.<ResourceConstraintInstance>builder()
            .clazz(ResourceConstraintInstance.class)
            .fieldName(ResourceConstraintInstanceKeys.nextIteration)
            .filterExpander(q -> q.field(ResourceConstraintInstanceKeys.state).in(NOT_FINISHED_STATES))
            .targetInterval(ofSeconds(30))
            .acceptableNoAlertDelay(ofSeconds(30))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(ResourceConstraintInstance instance) {
    String constraintId = instance.getResourceConstraintId();
    boolean toUnblock = false;
    try {
      if (State.BLOCKED.name().equals(instance.getState())) {
        if (logger.isWarnEnabled()) {
          logger.error("This is a completely blocked constraint: {}", constraintId);
        }
        toUnblock = true;
      } else if (State.ACTIVE.name().equals(instance.getState())) {
        if (resourceConstraintService.updateActiveConstraintForInstance(instance)) {
          logger.info("The following resource constrained need to be unblocked: {}", constraintId);
          toUnblock = true;
        }
      }
      if (toUnblock) {
        // Unblock the constraints that can be unblocked
        resourceConstraintService.updateBlockedConstraints(Sets.newHashSet(constraintId));
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException e) {
      logger.error("", e);
    }
  }
}
