package io.harness.workers.background.critical.iterator;

import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.EXPIRED;
import static io.harness.beans.ExecutionStatus.flowingStatuses;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.MARK_EXPIRED;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

@Singleton
@Slf4j
public class WorkflowExecutionMonitorHandler implements Handler<WorkflowExecution> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private ExecutorService executorService;
  @Inject private StateMachineExecutor stateMachineExecutor;

  private static final Duration INACTIVITY_TIMEOUT = Duration.ofSeconds(30);

  public void registerIterators() {
    PumpExecutorOptions options = PumpExecutorOptions.builder()
                                      .interval(Duration.ofSeconds(10))
                                      .poolSize(5)
                                      .name("WorkflowExecutionMonitor")
                                      .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(options, WorkflowExecution.class,
        MongoPersistenceIterator.<WorkflowExecution>builder()
            .clazz(WorkflowExecution.class)
            .fieldName(WorkflowExecutionKeys.nextIteration)
            .filterExpander(q -> q.field(WorkflowExecutionKeys.status).in(flowingStatuses()))
            .targetInterval(Duration.ofMinutes(1))
            .acceptableNoAlertDelay(Duration.ofSeconds(30))
            .handler(this)
            .schedulingType(SchedulingType.REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(WorkflowExecution entity) {
    try {
      boolean hasActiveStates = false;
      try (HIterator<StateExecutionInstance> stateExecutionInstances =
               new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                   .filter(StateExecutionInstanceKeys.appId, entity.getAppId())
                                   .filter(StateExecutionInstanceKeys.executionUuid, entity.getUuid())
                                   .field(StateExecutionInstanceKeys.status)
                                   .in(flowingStatuses())
                                   .fetch())) {
        hasActiveStates = stateExecutionInstances.hasNext();
        while (stateExecutionInstances.hasNext()) {
          StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
          if (stateExecutionInstance.getExpiryTs() > System.currentTimeMillis()) {
            continue;
          }

          logger.info("Expired StateExecutionInstance found: {}", stateExecutionInstance.getUuid());
          ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                      .executionInterruptType(MARK_EXPIRED)
                                                      .appId(stateExecutionInstance.getAppId())
                                                      .executionUuid(stateExecutionInstance.getExecutionUuid())
                                                      .stateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                      .build();

          executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
        }
      } catch (WingsException exception) {
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception e) {
        logger.error("Error in cleaning up the workflow execution {}", entity.getUuid(), e);
      }

      if (!hasActiveStates) {
        logger.warn("WorkflowExecution {} is in non final state, but there is no active state execution for it.",
            entity.getUuid());

        final StateExecutionInstance stateExecutionInstance =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(StateExecutionInstanceKeys.appId, entity.getAppId())
                .filter(StateExecutionInstanceKeys.executionUuid, entity.getUuid())
                .field(StateExecutionInstanceKeys.notifyId)
                .doesNotExist()
                .field(StateExecutionInstanceKeys.callback)
                .exists()
                .order(Sort.descending(StateExecutionInstanceKeys.lastUpdatedAt))
                .get();

        if (stateExecutionInstance == null) {
          logger.error("Workflow execution stuck, but we cannot find good state to callback from. This is so wrong!");
          return;
        }

        if (stateExecutionInstance.getLastUpdatedAt() > System.currentTimeMillis() - INACTIVITY_TIMEOUT.toMillis()) {
          logger.warn("WorkflowExecution {} last callbackable state {} is very recent."
                  + "Lets give more time to the system it might be just in the middle of things.",
              entity.getUuid(), stateExecutionInstance.getUuid());
          return;
        }

        final ExecutionContextImpl executionContext =
            stateMachineExecutor.getExecutionContext(stateExecutionInstance.getAppId(),
                stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid());

        try (AutoLogContext ignore = executionContext.autoLogContext()) {
          boolean expired = entity.getCreatedAt() < System.currentTimeMillis() - WorkflowExecution.EXPIRY.toMillis();
          // We lost the eventual exception, but its better than doing nothing
          stateMachineExecutor.executeCallback(
              executionContext, stateExecutionInstance, expired ? EXPIRED : ERROR, null);
        }
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error in monitoring the workflow execution {}", entity.getUuid());
    }
  }
}
