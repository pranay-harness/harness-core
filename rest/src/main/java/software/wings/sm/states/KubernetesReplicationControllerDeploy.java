package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.utils.KubernetesConvention.getReplicationControllerNamePrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.GcpConfig;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesReplicationControllerDeploy extends ContainerServiceDeploy {
  @Attributes(title = "Desired Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

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
  protected Optional<Integer> getServiceDesiredCount(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, String region, ContainerServiceElement containerServiceElement) {
    if (isNotEmpty(containerServiceElement.getName())) {
      // TODO - sync task?

      ReplicationController replicationController = kubernetesContainerService.getController(
          getKubernetesConfig(settingAttribute, encryptedDataDetails, containerServiceElement), encryptedDataDetails,
          containerServiceElement.getName());
      if (replicationController != null) {
        return Optional.of(replicationController.getSpec().getReplicas());
      }
    }
    return Optional.empty();
  }

  @Override
  protected LinkedHashMap<String, Integer> getActiveServiceCounts(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, String region, ContainerServiceElement containerServiceElement) {
    // TODO - sync task?

    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    ReplicationControllerList replicationControllers = kubernetesContainerService.listControllers(
        getKubernetesConfig(settingAttribute, encryptedDataDetails, containerServiceElement), encryptedDataDetails);
    if (replicationControllers != null) {
      String controllerNamePrefix =
          getReplicationControllerNamePrefixFromControllerName(containerServiceElement.getName());
      List<ReplicationController> activeOldReplicationControllers =
          replicationControllers.getItems()
              .stream()
              .filter(c -> c.getMetadata().getName().startsWith(controllerNamePrefix) && c.getSpec().getReplicas() > 0)
              .collect(Collectors.toList());
      activeOldReplicationControllers.sort(
          Comparator.comparingInt(rc -> getRevisionFromControllerName(rc.getMetadata().getName())));
      activeOldReplicationControllers.forEach(rc -> result.put(rc.getMetadata().getName(), rc.getSpec().getReplicas()));
    }
    return result;
  }

  private KubernetesConfig getKubernetesConfig(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerServiceElement containerServiceElement) {
    return settingAttribute.getValue() instanceof GcpConfig
        ? gkeClusterService.getCluster(settingAttribute, encryptedDataDetails, containerServiceElement.getClusterName(),
              containerServiceElement.getNamespace())
        : (KubernetesConfig) settingAttribute.getValue();
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
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  @Override
  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  public static final class KubernetesReplicationControllerDeployBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private String commandName;
    private int instanceCount;
    private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

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

    public KubernetesReplicationControllerDeployBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public KubernetesReplicationControllerDeploy build() {
      KubernetesReplicationControllerDeploy kubernetesReplicationControllerDeploy =
          new KubernetesReplicationControllerDeploy(name);
      kubernetesReplicationControllerDeploy.setId(id);
      kubernetesReplicationControllerDeploy.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerDeploy.setStateType(stateType);
      kubernetesReplicationControllerDeploy.setRollback(false);
      kubernetesReplicationControllerDeploy.setCommandName(commandName);
      kubernetesReplicationControllerDeploy.setInstanceCount(instanceCount);
      kubernetesReplicationControllerDeploy.setInstanceUnitType(instanceUnitType);
      return kubernetesReplicationControllerDeploy;
    }
  }
}
