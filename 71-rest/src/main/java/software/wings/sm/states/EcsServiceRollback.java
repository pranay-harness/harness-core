package software.wings.sm.states;

import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

/**
 * Created by brett on 3/24/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsServiceRollback extends ContainerServiceDeploy {
  @Attributes(title = "Rollback all phases at once") private boolean rollbackAllPhases;

  private String commandName = "Resize Service Cluster";

  public EcsServiceRollback(String name) {
    super(name, StateType.ECS_SERVICE_ROLLBACK.name());
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
    return anEcsResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withRegion(contextData.region)
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .withContainerServiceName(contextData.containerElement.getName())
        .withResizeStrategy(contextData.containerElement.getResizeStrategy())
        .withUseFixedInstances(contextData.containerElement.isUseFixedInstances())
        .withMaxInstances(contextData.containerElement.getMaxInstances())
        .withFixedInstances(contextData.containerElement.getFixedInstances())
        .withNewInstanceData(contextData.rollbackElement.getNewInstanceData())
        .withOldInstanceData(contextData.rollbackElement.getOldInstanceData())
        .withOriginalServiceCounts(contextData.containerElement.getActiveServiceCounts())
        .withRollback(true)
        .withRollbackAllPhases(rollbackAllPhases)
        .build();
  }
}
