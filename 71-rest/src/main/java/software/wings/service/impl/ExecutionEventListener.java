package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.FAILED;
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
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.KubernetesInfrastructure;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;

@Slf4j
public class ExecutionEventListener extends QueueListener<ExecutionEvent> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InfrastructureDefinitionService infraDefinitionService;
  @Inject private AppService appService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  public ExecutionEventListener() {
    super(false);
  }

  @Override
  public void onMessage(ExecutionEvent message) {
    String lockId;
    boolean infraRefactor = featureFlagService.isEnabled(
        FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(message.getAppId()));
    if (infraRefactor) {
      lockId = message.getWorkflowId()
          + (isNotEmpty(message.getInfraDefinitionIds())
                    ? "|" + Joiner.on("|").join(Ordering.natural().sortedCopy(message.getInfraDefinitionIds()))
                    : "");
    } else {
      lockId = message.getWorkflowId()
          + (isNotEmpty(message.getInfraMappingIds())
                    ? "|" + Joiner.on("|").join(Ordering.natural().sortedCopy(message.getInfraMappingIds()))
                    : "");
    }

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

      if (!infraRefactor) {
        if (isNotEmpty(message.getInfraMappingIds())) {
          runningQuery.field(WorkflowExecutionKeys.infraMappingIds).in(message.getInfraMappingIds());
        }
      } else {
        if (isNotEmpty(message.getInfraDefinitionIds())) {
          runningQuery.field(WorkflowExecutionKeys.infraDefinitionIds).in(message.getInfraDefinitionIds());
        }
      }

      WorkflowExecution runningWorkflowExecutions = runningQuery.get();

      if (runningWorkflowExecutions != null) {
        boolean namespaceExpression =
            infraRefactor ? isNamespaceExpressionInfraRefactor(message) : isNamespaceExpression(message);
        if (!namespaceExpression) {
          return;
        }
      }

      final Query<WorkflowExecution> queueQuery = wingsPersistence.createQuery(WorkflowExecution.class)
                                                      .filter(WorkflowExecutionKeys.appId, message.getAppId())
                                                      .filter(WorkflowExecutionKeys.workflowId, message.getWorkflowId())
                                                      .filter(WorkflowExecutionKeys.status, QUEUED)
                                                      .order(Sort.ascending(WorkflowExecutionKeys.createdAt));

      if (!infraRefactor) {
        if (isNotEmpty(message.getInfraMappingIds())) {
          queueQuery.field(WorkflowExecutionKeys.infraMappingIds).in(message.getInfraMappingIds());
        }
      } else {
        if (isNotEmpty(message.getInfraDefinitionIds())) {
          queueQuery.field(WorkflowExecutionKeys.infraDefinitionIds).in(message.getInfraDefinitionIds());
        }
      }

      WorkflowExecution workflowExecution = queueQuery.get();
      if (workflowExecution == null) {
        return;
      }

      try (WorkflowExecutionLogContext ctx = new WorkflowExecutionLogContext(workflowExecution.getUuid())) {
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

  boolean isNamespaceExpressionInfraRefactor(ExecutionEvent message) {
    boolean namespaceExpression = false;
    if (message.getInfraDefinitionIds() != null) {
      for (String infraId : message.getInfraDefinitionIds()) {
        InfrastructureDefinition infrastructureDefinition = infraDefinitionService.get(message.getAppId(), infraId);
        InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
        boolean isContainerBasedInfra = infrastructure instanceof KubernetesInfrastructure;

        if (isContainerBasedInfra
            && containsVariablePattern(((KubernetesInfrastructure) infrastructure).getNamespace())) {
          namespaceExpression = true;
        }
      }
    }
    return namespaceExpression;
  }
}
