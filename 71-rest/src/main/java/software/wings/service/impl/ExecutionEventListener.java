package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.NEW;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.ExpressionEvaluator.containsVariablePattern;
import static java.util.Arrays.asList;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.queue.QueueListener;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;

public class ExecutionEventListener extends QueueListener<ExecutionEvent> {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionEventListener.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private InfrastructureMappingService infrastructureMappingService;
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

      final Query<WorkflowExecution> runningQuery =
          wingsPersistence.createQuery(WorkflowExecution.class)
              .filter(WorkflowExecution.APP_ID_KEY, message.getAppId())
              .filter(WorkflowExecution.WORKFLOW_ID_KEY, message.getWorkflowId())
              .field(WorkflowExecution.STATUS_KEY)
              .in(asList(RUNNING, PAUSED))
              .project(WorkflowExecution.UUID_KEY, true);

      if (isNotEmpty(message.getInfraMappingIds())) {
        runningQuery.field(WorkflowExecution.INFRA_MAPPING_IDS_KEY).in(message.getInfraMappingIds());
      }

      WorkflowExecution runningWorkflowExecutions = runningQuery.get();

      if (runningWorkflowExecutions != null) {
        boolean namespaceExpression = false;
        if (message.getInfraMappingIds() != null) {
          for (String infraId : message.getInfraMappingIds()) {
            InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(message.getAppId(), infraId);
            if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
                && containsVariablePattern(
                       ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace())) {
              namespaceExpression = true;
              break;
            }
            if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping
                && containsVariablePattern(
                       ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getNamespace())) {
              namespaceExpression = true;
              break;
            }
            if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
                && containsVariablePattern(
                       ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace())) {
              namespaceExpression = true;
              break;
            }
          }
        }

        if (!namespaceExpression) {
          return;
        }
      }

      final Query<WorkflowExecution> queueQuery =
          wingsPersistence.createQuery(WorkflowExecution.class)
              .filter(WorkflowExecution.APP_ID_KEY, message.getAppId())
              .filter(WorkflowExecution.WORKFLOW_ID_KEY, message.getWorkflowId())
              .filter(WorkflowExecution.STATUS_KEY, QUEUED)
              .order(Sort.ascending(WorkflowExecution.CREATED_AT_KEY));
      if (isNotEmpty(message.getInfraMappingIds())) {
        queueQuery.field(WorkflowExecution.INFRA_MAPPING_IDS_KEY).in(message.getInfraMappingIds());
      }

      WorkflowExecution workflowExecution = queueQuery.get();
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
                                             .filter(WorkflowExecution.APP_ID_KEY, workflowExecution.getAppId())
                                             .filter(WorkflowExecution.ID_KEY, workflowExecution.getUuid())
                                             .field(WorkflowExecution.STATUS_KEY)
                                             .in(asList(NEW, QUEUED));
        UpdateOperations<WorkflowExecution> updateOps =
            wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                .set(WorkflowExecution.STATUS_KEY, status)
                .set(WorkflowExecution.START_TS_KEY, System.currentTimeMillis());
        wingsPersistence.update(query, updateOps);
      } catch (Exception e) {
        logger.error("Exception in generating execution log context", e);
      }
    }
  }
}
