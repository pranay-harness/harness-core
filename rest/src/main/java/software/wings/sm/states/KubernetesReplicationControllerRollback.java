package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.SettingAttribute;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.LinkedHashMap;
import java.util.Optional;

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
  protected Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String region, ContainerServiceElement containerServiceElement) {
    return Optional.empty();
  }

  @Override
  protected LinkedHashMap<String, Integer> getActiveServiceCounts(
      SettingAttribute settingAttribute, String region, ContainerServiceElement containerServiceElement) {
    return new LinkedHashMap<>();
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
}
