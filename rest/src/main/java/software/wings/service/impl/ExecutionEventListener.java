package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;

public class ExecutionEventListener extends AbstractQueueListener<ExecutionEvent> {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionEventListener.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private StateMachineExecutor stateMachineExecutor;
  public ExecutionEventListener() {
    super(false);
  }

  @Override
  protected void onMessage(ExecutionEvent message) {
    String lockId = message.getWorkflowId()
        + (isNotEmpty(message.getInfraMappingIds())
                  ? "|" + Joiner.on("|").join(Ordering.natural().sortedCopy(message.getInfraMappingIds()))
                  : "");

    try (AcquiredLock lock = persistentLocker.tryToAcquireLock(Workflow.class, lockId, Duration.ofMinutes(1))) {
      if (lock == null) {
        return;
      }
      logger.info("Acquired the lock {}. Verifying to see if the execution can be started", lockId);
      PageRequestBuilder pageRequestBuilder =
          aPageRequest()
              .addFilter(WorkflowExecution.APP_ID_KEY, EQ, message.getAppId())
              .addFilter(WorkflowExecution.STATUS_KEY, Operator.IN, RUNNING, PAUSED)
              .addFieldsIncluded("uuid")
              .addFilter(WorkflowExecution.WORKFLOW_ID_KEY, EQ, message.getWorkflowId())
              .withLimit("1");

      if (isNotEmpty(message.getInfraMappingIds())) {
        pageRequestBuilder.addFilter(
            WorkflowExecution.INFRAMAPPING_IDS_KEY, Operator.IN, message.getInfraMappingIds().toArray());
      }

      PageResponse<WorkflowExecution> runningWorkflowExecutions =
          wingsPersistence.query(WorkflowExecution.class, pageRequestBuilder.build());

      if (isNotEmpty(runningWorkflowExecutions)) {
        return;
      }

      pageRequestBuilder = aPageRequest()
                               .addFilter(WorkflowExecution.APP_ID_KEY, EQ, message.getAppId())
                               .addFilter(WorkflowExecution.STATUS_KEY, EQ, QUEUED)
                               .addOrder(WorkflowExecution.CREATED_AT_KEY, OrderType.ASC)
                               .addFilter(WorkflowExecution.WORKFLOW_ID_KEY, EQ, message.getWorkflowId());

      if (isNotEmpty(message.getInfraMappingIds())) {
        pageRequestBuilder.addFilter(
            WorkflowExecution.INFRAMAPPING_IDS_KEY, Operator.IN, message.getInfraMappingIds().toArray());
      }

      WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, pageRequestBuilder.build());
      if (workflowExecution == null) {
        return;
      }

      try (ExecutionLogContext ctx = new ExecutionLogContext(workflowExecution.getUuid())) {
        logger.info("Starting Queued execution..");

        boolean started = stateMachineExecutor.startQueuedExecution(message.getAppId(), workflowExecution.getUuid());
        ExecutionStatus status = RUNNING;
        if (!started) {
          status = FAILED;
          logger.error("WorkflowExecution could not be started from QUEUED state- appId:{}, WorkflowExecution:{}",
              message.getAppId(), workflowExecution.getUuid());
        }

        // TODO: findAndModify
        Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                             .filter("appId", workflowExecution.getAppId())
                                             .filter(ID_KEY, workflowExecution.getUuid())
                                             .field("status")
                                             .in(asList(NEW, QUEUED));
        UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                            .set("status", status)
                                                            .set("startTs", System.currentTimeMillis());
        wingsPersistence.update(query, updateOps);
      } catch (Exception e) {
        logger.error("Exception in generating execution log context", e);
      }
    }
  }
}
