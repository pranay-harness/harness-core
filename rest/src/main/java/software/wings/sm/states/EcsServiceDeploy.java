package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Created by rishi on 2/8/17.
 */
public class EcsServiceDeploy extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Service Cluster")
  private String commandName;

  @Inject @Transient private transient AwsClusterService awsClusterService;

  public EcsServiceDeploy(String name) {
    super(name, StateType.ECS_SERVICE_DEPLOY.name());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String clusterName, @Nullable String serviceName) {
    if (StringUtils.isNotEmpty(serviceName)) {
      List<com.amazonaws.services.ecs.model.Service> services =
          awsClusterService.getServices(settingAttribute, clusterName);
      Optional<com.amazonaws.services.ecs.model.Service> ecsService =
          services.stream().filter(svc -> svc.getServiceName().equals(serviceName)).findFirst();
      if (ecsService.isPresent()) {
        return Optional.of(ecsService.get().getDesiredCount());
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

  public static final class EcsServiceDeployBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String commandName;
    private int instanceCount;

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

    public EcsServiceDeployBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
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

    public EcsServiceDeploy build() {
      EcsServiceDeploy ecsServiceDeploy = new EcsServiceDeploy(name);
      ecsServiceDeploy.setId(id);
      ecsServiceDeploy.setRequiredContextElementType(requiredContextElementType);
      ecsServiceDeploy.setStateType(stateType);
      ecsServiceDeploy.setRollback(rollback);
      ecsServiceDeploy.setCommandName(commandName);
      ecsServiceDeploy.setInstanceCount(instanceCount);
      return ecsServiceDeploy;
    }
  }
}
