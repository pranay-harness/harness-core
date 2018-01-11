package software.wings.sm.states;

import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;

import com.github.reinert.jjschema.Attributes;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;

/**
 * Created by brett on 4/24/17
 */
public class KubernetesReplicationControllerRollback extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Replication Controller")
  private String commandName = "Resize Replication Controller";

  public KubernetesReplicationControllerRollback(String name) {
    super(name, StateType.KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK.name());
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
    return aKubernetesResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withDesiredCounts(desiredCounts)
        .withNamespace(contextData.containerElement.getNamespace())
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .build();
  }
}
