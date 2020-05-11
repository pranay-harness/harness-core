package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.ExpressionEvaluator.containsVariablePattern;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;
import static java.util.Arrays.asList;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AutoLogContext;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;

@OwnedBy(CDC)
@Slf4j
public class ExecutionEventListener extends QueueListener<ExecutionEvent> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AppService appService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject
  public ExecutionEventListener(QueueConsumer<ExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(ExecutionEvent message) {
    boolean infraRefactor = featureFlagService.isEnabled(
        FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(message.getAppId()));
    if (infraRefactor) {
      return;
    }
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
              .filter(WorkflowExecutionKeys.appId, message.getAppId())
              .filter(WorkflowExecutionKeys.workflowId, message.getWorkflowId())
              .field(WorkflowExecutionKeys.status)
              .in(asList(RUNNING, PAUSED))
              .project(WorkflowExecutionKeys.uuid, true);

      if (isNotEmpty(message.getInfraMappingIds())) {
        runningQuery.field(WorkflowExecutionKeys.infraMappingIds).in(message.getInfraMappingIds());
      }

      WorkflowExecution runningWorkflowExecutions = runningQuery.get();

      if (runningWorkflowExecutions != null) {
        boolean namespaceExpression = isNamespaceExpression(message);
        if (!namespaceExpression) {
          return;
        }
      }

      final Query<WorkflowExecution> queueQuery = wingsPersistence.createQuery(WorkflowExecution.class)
                                                      .filter(WorkflowExecutionKeys.appId, message.getAppId())
                                                      .filter(WorkflowExecutionKeys.workflowId, message.getWorkflowId())
                                                      .filter(WorkflowExecutionKeys.status, QUEUED)
                                                      .order(Sort.ascending(WorkflowExecutionKeys.createdAt));
      if (isNotEmpty(message.getInfraMappingIds())) {
        queueQuery.field(WorkflowExecutionKeys.infraMappingIds).in(message.getInfraMappingIds());
      }

      WorkflowExecution workflowExecution = queueQuery.get();
      if (workflowExecution == null) {
        return;
      }

      // We are about to unblock the next queued execution. Switch the MDC to it
      try (AutoLogContext ignore = new WorkflowExecutionLogContext(workflowExecution.getUuid(), OVERRIDE_NESTS)) {
        logger.info("Starting Queued execution..");

        boolean started = stateMachineExecutor.startQueuedExecution(message.getAppId(), workflowExecution.getUuid());
        ExecutionStatus status = RUNNING;
        if (!started) {
          status = FAILED;
          logger.error("WorkflowExecution could not be started from QUEUED state- appId:{}, WorkflowExecution:{}",
              message.getAppId(), workflowExecution.getUuid());
        }

        workflowExecutionService.updateStartStatus(workflowExecution.getAppId(), workflowExecution.getUuid(), status);
      } catch (Exception e) {
        logger.error("Exception in generating execution log context", e);
      }
    }
  }

  boolean isNamespaceExpression(ExecutionEvent message) {
    boolean namespaceExpression = false;
    if (message.getInfraMappingIds() != null) {
      for (String infraId : message.getInfraMappingIds()) {
        InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(message.getAppId(), infraId);
        if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
            && containsVariablePattern(((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace())) {
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
            && containsVariablePattern(((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace())) {
          namespaceExpression = true;
          break;
        }
      }
    }
    return namespaceExpression;
  }
}
