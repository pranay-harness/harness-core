package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;

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
import software.wings.dl.PageRequest;
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
    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(Workflow.class, message.getWorkflowId(), Duration.ofMinutes(1))) {
      if (lock == null) {
        return;
      }

      PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                       .addFilter("appId", EQ, message.getAppId())
                                                       .addFilter("workflowId", EQ, message.getWorkflowId())
                                                       .addFilter("status", Operator.IN, RUNNING, PAUSED)
                                                       .addFieldsIncluded("uuid")
                                                       .withLimit("1")
                                                       .build();

      PageResponse<WorkflowExecution> runningWorkflowExecutions =
          wingsPersistence.query(WorkflowExecution.class, pageRequest);
      if (isNotEmpty(runningWorkflowExecutions)) {
        return;
      }

      pageRequest = aPageRequest()
                        .addFilter("appId", EQ, message.getAppId())
                        .addFilter("workflowId", EQ, message.getWorkflowId())
                        .addFilter("status", EQ, QUEUED)
                        .addOrder("createdAt", OrderType.ASC)
                        .build();

      WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, pageRequest);
      if (workflowExecution == null) {
        return;
      }

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
    }
  }
}
