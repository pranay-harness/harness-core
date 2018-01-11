package software.wings.sm.states;

import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;

import com.github.reinert.jjschema.Attributes;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;

/**
 * Created by brett on 3/24/17
 */
public class EcsServiceRollback extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
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
  public int getInstanceCount() {
    return 0;
  }

  @Override
  public InstanceUnitType getInstanceUnitType() {
    return null;
  }

  @Override
  protected ContainerResizeParams buildContainerResizeParams(
      ContextData contextData, List<ContainerServiceData> desiredCounts) {
    return anEcsResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withDesiredCounts(desiredCounts)
        .withRegion(contextData.region)
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .build();
  }
}
