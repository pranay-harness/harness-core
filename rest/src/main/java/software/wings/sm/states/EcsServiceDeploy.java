package software.wings.sm.states;

import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.Service;
import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Created by rishi on 2/8/17.
 */
public class EcsServiceDeploy extends ContainerServiceDeploy {
  private static final Logger logger = LoggerFactory.getLogger(EcsServiceDeploy.class);

  @Attributes(title = "Number of instances") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percentage)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Service Cluster")
  private String commandName;

  @Inject @Transient private transient AwsClusterService awsClusterService;

  @Inject @Transient private transient EcsContainerService ecsContainerService;

  public EcsServiceDeploy(String name) {
    super(name, StateType.ECS_SERVICE_DEPLOY.name());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String region, String clusterName, @Nullable String serviceName) {
    if (StringUtils.isNotEmpty(serviceName)) {
      Optional<Service> service = awsClusterService.getServices(region, settingAttribute, clusterName)
                                      .stream()
                                      .filter(svc -> svc.getServiceName().equals(serviceName))
                                      .findFirst();
      if (service.isPresent()) {
        return Optional.of(service.get().getDesiredCount());
      }
    }
    return Optional.empty();
  }

  @Override
  protected void cleanup(SettingAttribute settingAttribute, String region, String clusterName, String serviceName) {
    int revision = getRevisionFromServiceName(serviceName);
    if (revision > ContainerServiceDeploy.KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision - ContainerServiceDeploy.KEEP_N_REVISIONS;
      String serviceNamePrefix = getServiceNamePrefixFromServiceName(serviceName);
      List<Service> services = ecsContainerService.getServices(region, settingAttribute, clusterName);
      for (Service service :
          services.stream()
              .filter(s -> s.getServiceName().startsWith(serviceNamePrefix) && s.getDesiredCount() == 0)
              .collect(Collectors.toList())) {
        String oldServiceName = service.getServiceName();
        if (getRevisionFromServiceName(oldServiceName) < minRevisionToKeep) {
          logger.info("Deleting old version: " + oldServiceName);
          ecsContainerService.deleteService(region, settingAttribute, clusterName, oldServiceName);
        }
      }
    }
  }

  @Override
  public int fetchDesiredCount(Integer previousDesiredCount) {
    if (instanceUnitType != null && instanceUnitType == InstanceUnitType.PERCENTAGE) {
      // TODO: take care of previous occurrence and ensure total does not exceed previousDesiredCount
      int realCount = (getInstanceCount() * previousDesiredCount) / 100;
      if (realCount < 1) {
        realCount = 1;
      }
      return realCount;
    } else {
      return getInstanceCount();
    }
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
