package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.Service;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
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
 * Created by rishi on 2/8/17.
 */
public class EcsServiceDeploy extends ContainerServiceDeploy {
  @Attributes(title = "Desired Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Service Cluster")
  private String commandName;

  @Inject @Transient private transient AwsClusterService awsClusterService;

  public EcsServiceDeploy(String name) {
    super(name, StateType.ECS_SERVICE_DEPLOY.name());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, String region, ContainerServiceElement containerServiceElement) {
    if (isNotEmpty(containerServiceElement.getName())) {
      Optional<Service> service =
          awsClusterService
              .getServices(region, settingAttribute, encryptedDataDetails, containerServiceElement.getClusterName())
              .stream()
              .filter(svc -> svc.getServiceName().equals(containerServiceElement.getName()))
              .findFirst();
      if (service.isPresent()) {
        return Optional.of(service.get().getDesiredCount());
      }
    }
    return Optional.empty();
  }

  @Override
  protected LinkedHashMap<String, Integer> getActiveServiceCounts(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, String region, ContainerServiceElement containerServiceElement) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceElement.getName());
    List<Service> activeOldServices =
        awsClusterService
            .getServices(region, settingAttribute, encryptedDataDetails, containerServiceElement.getClusterName())
            .stream()
            .filter(service -> service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0)
            .collect(Collectors.toList());
    activeOldServices.sort(Comparator.comparingInt(service -> getRevisionFromServiceName(service.getServiceName())));
    activeOldServices.forEach(service -> result.put(service.getServiceName(), service.getDesiredCount()));
    return result;
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

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

  public static final class EcsServiceDeployBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private String commandName;
    private int instanceCount;
    private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

    private EcsServiceDeployBuilder(String name) {
      this.name = name;
    }

    public static EcsServiceDeployBuilder anEcsServiceDeploy(String name) {
      return new EcsServiceDeployBuilder(name);
    }

    public EcsServiceDeployBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public EcsServiceDeployBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public EcsServiceDeployBuilder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public EcsServiceDeployBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public EcsServiceDeployBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public EcsServiceDeployBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public EcsServiceDeployBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public EcsServiceDeploy build() {
      EcsServiceDeploy ecsServiceDeploy = new EcsServiceDeploy(name);
      ecsServiceDeploy.setId(id);
      ecsServiceDeploy.setRequiredContextElementType(requiredContextElementType);
      ecsServiceDeploy.setStateType(stateType);
      ecsServiceDeploy.setRollback(false);
      ecsServiceDeploy.setCommandName(commandName);
      ecsServiceDeploy.setInstanceCount(instanceCount);
      ecsServiceDeploy.setInstanceUnitType(instanceUnitType);
      return ecsServiceDeploy;
    }
  }
}
