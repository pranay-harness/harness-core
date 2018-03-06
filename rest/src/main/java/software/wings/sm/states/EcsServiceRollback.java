package software.wings.sm.states;

import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;

import com.github.reinert.jjschema.Attributes;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

/**
 * Created by brett on 3/24/17
 */
public class EcsServiceRollback extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @DefaultValue("Resize Service Cluster")
  private String commandName = "Resize Service Cluster";

  public EcsServiceRollback(String name) {
    super(name, StateType.ECS_SERVICE_ROLLBACK.name());
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
    return anEcsResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withRegion(contextData.region)
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
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
