package software.wings.sm.states;

import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;

import com.github.reinert.jjschema.Attributes;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.stencils.DefaultValue;

/**
 * Created by brett on 4/24/17
 */
public class KubernetesDeployRollback extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @DefaultValue("Resize Replication Controller")
  private String commandName = "Resize Replication Controller";

  public KubernetesDeployRollback(String name) {
    super(name, KUBERNETES_DEPLOY_ROLLBACK.name());
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
  protected ContainerResizeParams buildContainerResizeParams(ContextData contextData) {
    return aKubernetesResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withNamespace(contextData.containerElement.getNamespace())
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .withUseAutoscaler(contextData.containerElement.isUseAutoscaler())
        .withRollbackAutoscaler(true)
        .withSubscriptionId(contextData.subscriptionId)
        .withResourceGroup(contextData.resourceGroup)
        .withUseIstioRouteRule(contextData.containerElement.isUseIstioRouteRule())
        .withRollback(isRollback())
        .withInstanceCount(contextData.instanceCount)
        .withInstanceUnitType(getInstanceUnitType())
        .withContainerServiceName(contextData.containerElement.getName())
        .withResizeStrategy(contextData.containerElement.getResizeStrategy())
        .withUseFixedInstances(contextData.containerElement.isUseFixedInstances())
        .withMaxInstances(contextData.containerElement.getMaxInstances())
        .withFixedInstances(contextData.containerElement.getFixedInstances())
        .withNewInstanceData(contextData.rollbackElement.getNewInstanceData())
        .withOldInstanceData(contextData.rollbackElement.getOldInstanceData())
        .build();
  }
}
