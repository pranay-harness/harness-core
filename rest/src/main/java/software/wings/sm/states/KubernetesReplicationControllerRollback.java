package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Created by brett on 4/24/17
 */
public class KubernetesReplicationControllerRollback extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Replication Controller")
  private String commandName = "Resize Replication Controller";

  @Inject @Transient private transient GkeClusterService gkeClusterService;

  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;

  public KubernetesReplicationControllerRollback(String name) {
    super(name, StateType.KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK.name());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String region, String clusterName, @Nullable String serviceName) {
    return Optional.empty();
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
  public int fetchDesiredCount(int lastDeploymentDesiredCount) {
    return 0;
  }

  public static final class KubernetesReplicationControllerRollbackBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private String commandName;

    private KubernetesReplicationControllerRollbackBuilder(String name) {
      this.name = name;
    }

    public static KubernetesReplicationControllerRollbackBuilder aKubernetesReplicationControllerRollback(String name) {
      return new KubernetesReplicationControllerRollbackBuilder(name);
    }

    public KubernetesReplicationControllerRollbackBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public KubernetesReplicationControllerRollbackBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesReplicationControllerRollbackBuilder withRequiredContextElementType(
        ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public KubernetesReplicationControllerRollbackBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public KubernetesReplicationControllerRollbackBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public KubernetesReplicationControllerRollback build() {
      KubernetesReplicationControllerRollback kubernetesReplicationControllerRollback =
          new KubernetesReplicationControllerRollback(name);
      kubernetesReplicationControllerRollback.setId(id);
      kubernetesReplicationControllerRollback.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerRollback.setStateType(stateType);
      kubernetesReplicationControllerRollback.setRollback(true);
      kubernetesReplicationControllerRollback.setCommandName(commandName);
      return kubernetesReplicationControllerRollback;
    }
  }
}
