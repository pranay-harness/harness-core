package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.KubernetesConfig;
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
 * Created by brett on 3/1/17
 */
public class KubernetesReplicationControllerDeploy extends CloudServiceDeploy {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Replication Controller")
  private String commandName;

  @Inject @Transient private transient GkeClusterService gkeClusterService;

  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;

  public KubernetesReplicationControllerDeploy(String name) {
    super(name, StateType.KUBERNETES_REPLICATION_CONTROLLER_DEPLOY.name());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String clusterName, @Nullable String serviceName) {
    if (StringUtils.isNotEmpty(serviceName)) {
      KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(settingAttribute, clusterName);
      ReplicationController replicationController =
          kubernetesContainerService.getController(kubernetesConfig, serviceName);
      if (replicationController != null) {
        return Optional.of(replicationController.getSpec().getReplicas());
      }
    }
    return Optional.empty();
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public static final class KubernetesReplicationControllerDeployBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String commandName;
    private int instanceCount;

    private KubernetesReplicationControllerDeployBuilder(String name) {
      this.name = name;
    }

    public static KubernetesReplicationControllerDeployBuilder aKubernetesReplicationControllerDeploy(String name) {
      return new KubernetesReplicationControllerDeployBuilder(name);
    }

    public KubernetesReplicationControllerDeployBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withRequiredContextElementType(
        ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public KubernetesReplicationControllerDeploy build() {
      KubernetesReplicationControllerDeploy kubernetesReplicationControllerDeploy =
          new KubernetesReplicationControllerDeploy(name);
      kubernetesReplicationControllerDeploy.setId(id);
      kubernetesReplicationControllerDeploy.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerDeploy.setStateType(stateType);
      kubernetesReplicationControllerDeploy.setRollback(rollback);
      kubernetesReplicationControllerDeploy.setCommandName(commandName);
      kubernetesReplicationControllerDeploy.setInstanceCount(instanceCount);
      return kubernetesReplicationControllerDeploy;
    }
  }
}
