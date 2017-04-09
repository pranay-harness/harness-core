package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.KubernetesReplicationControllerElement;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesReplicationControllerDeploy extends CloudServiceDeploy {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesReplicationControllerDeploy.class);

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
  protected String getClusterName(ExecutionContext context) {
    return context
        .<KubernetesReplicationControllerElement>getContextElement(ContextElementType.KUBERNETES_REPLICATION_CONTROLLER)
        .getClusterName();
  }

  @Override
  protected String getServiceName(ExecutionContext context) {
    return context
        .<KubernetesReplicationControllerElement>getContextElement(ContextElementType.KUBERNETES_REPLICATION_CONTROLLER)
        .getName();
  }

  @Override
  protected String getOldServiceName(ExecutionContext context) {
    return context
        .<KubernetesReplicationControllerElement>getContextElement(ContextElementType.KUBERNETES_REPLICATION_CONTROLLER)
        .getOldName();
  }

  @Override
  protected int getServiceDesiredCount(ExecutionContext context, SettingAttribute settingAttribute) {
    String replicationControllerName = getServiceName(context);
    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(settingAttribute, getClusterName(context));
    ReplicationController replicationController =
        kubernetesContainerService.getController(kubernetesConfig, replicationControllerName);

    if (replicationController == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          "Kubernetes replication controller setup not done, controllerName: " + replicationControllerName);
    }

    int desiredCount = replicationController.getSpec().getReplicas() + instanceCount;
    logger.info("Desired count for service {} is {}", replicationControllerName, desiredCount);

    return desiredCount;
  }

  @Override
  protected int getOldServiceDesiredCount(ExecutionContext context, SettingAttribute settingAttribute) {
    String replicationControllerName = getOldServiceName(context);
    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(settingAttribute, getClusterName(context));
    ReplicationController replicationController =
        kubernetesContainerService.getController(kubernetesConfig, replicationControllerName);
    if (replicationController == null) {
      logger.info("Old kubernetes replication controller {} does not exist.. nothing to do", replicationControllerName);
      return -1;
    }

    int desiredCount = Math.max(replicationController.getSpec().getReplicas() - instanceCount, 0);
    logger.info("Desired count for service {} is {}", replicationControllerName, desiredCount);
    return desiredCount;
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
