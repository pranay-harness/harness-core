package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.expression.ExpressionEvaluator.containsVariablePattern;

import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.PersistentLocker;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateMachineExecutor;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

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
  @Inject private WorkflowExecutionUpdate executionUpdate;

  @Inject
  public ExecutionEventListener(QueueConsumer<ExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(ExecutionEvent message) {
    // Removed implementation because INFRA_MAPPING_REFACTOR does not need this
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
