package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

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
  protected int getServiceDesiredCount(SettingAttribute settingAttribute, String clusterName, String serviceName) {
    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(settingAttribute, clusterName);
    ReplicationController replicationController =
        kubernetesContainerService.getController(kubernetesConfig, serviceName);
    if (replicationController == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          "Kubernetes replication controller setup not done, controllerName: " + serviceName);
    }
    return replicationController.getSpec().getReplicas();
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
