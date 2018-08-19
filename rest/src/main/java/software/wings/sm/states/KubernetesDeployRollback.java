package software.wings.sm.states;

import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;

import com.github.reinert.jjschema.Attributes;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.ExecutionContext;

/**
 * Created by brett on 4/24/17
 */
public class KubernetesDeployRollback extends ContainerServiceDeploy {
  @Attributes(title = "Rollback all phases at once") private boolean rollbackAllPhases;

  private String commandName = "Resize Replication Controller";

  public KubernetesDeployRollback(String name) {
    super(name, KUBERNETES_DEPLOY_ROLLBACK.name());
  }

  public boolean isRollbackAllPhases() {
    return rollbackAllPhases;
  }

  public void setRollbackAllPhases(boolean rollbackAllPhases) {
    this.rollbackAllPhases = rollbackAllPhases;
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  @Override
  public String getInstanceCount() {
    return "0";
  }

  @Override
  public InstanceUnitType getInstanceUnitType() {
    return null;
  }

  @Override
  public String getDownsizeInstanceCount() {
    return null;
  }

  @Override
  public InstanceUnitType getDownsizeInstanceUnitType() {
    return null;
  }

  @Override
  protected ContainerResizeParams buildContainerResizeParams(ExecutionContext context, ContextData contextData) {
    return aKubernetesResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withNamespace(contextData.containerElement.getNamespace())
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .withUseAutoscaler(contextData.containerElement.isUseAutoscaler())
        .withSubscriptionId(contextData.subscriptionId)
        .withResourceGroup(contextData.resourceGroup)
        .withUseIstioRouteRule(contextData.containerElement.isUseIstioRouteRule())
        .withContainerServiceName(contextData.containerElement.getName())
        .withResizeStrategy(contextData.containerElement.getResizeStrategy())
        .withUseFixedInstances(contextData.containerElement.isUseFixedInstances())
        .withMaxInstances(contextData.containerElement.getMaxInstances())
        .withFixedInstances(contextData.containerElement.getFixedInstances())
        .withNewInstanceData(contextData.rollbackElement.getNewInstanceData())
        .withOldInstanceData(contextData.rollbackElement.getOldInstanceData())
        .withOriginalServiceCounts(contextData.containerElement.getActiveServiceCounts())
        .withOriginalTrafficWeights(contextData.containerElement.getTrafficWeights())
        .withRollback(true)
        .withRollbackAllPhases(rollbackAllPhases)
        .build();
  }
}
