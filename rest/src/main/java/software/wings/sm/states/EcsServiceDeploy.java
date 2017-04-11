package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;
import java.util.Optional;

/**
 * Created by rishi on 2/8/17.
 */
public class EcsServiceDeploy extends CloudServiceDeploy {
  private static final Logger logger = LoggerFactory.getLogger(EcsServiceDeploy.class);

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Service Cluster")
  private String commandName;

  @Inject @Transient private transient AwsClusterService awsClusterService;

  public EcsServiceDeploy(String name) {
    super(name, StateType.ECS_SERVICE_DEPLOY.name());
  }

  @Override
  protected int getServiceDesiredCount(ExecutionContext context, SettingAttribute settingAttribute) {
    String ecsServiceName = getServiceName(context);
    List<com.amazonaws.services.ecs.model.Service> services =
        awsClusterService.getServices(settingAttribute, getClusterName(context));
    Optional<com.amazonaws.services.ecs.model.Service> ecsService =
        services.stream().filter(svc -> svc.getServiceName().equals(ecsServiceName)).findFirst();
    if (!ecsService.isPresent()) {
      throw new WingsException(
          ErrorCode.INVALID_REQUEST, "message", "ECS Service setup not done, ecsServiceName: " + ecsServiceName);
    }
    int desiredCount = ecsService.get().getDesiredCount() + instanceCount;
    logger.info("Desired count for service {} is {}", ecsServiceName, desiredCount);
    return desiredCount;
  }

  @Override
  protected int getOldServiceDesiredCount(ExecutionContext context, SettingAttribute settingAttribute) {
    String ecsServiceName = getOldServiceName(context);
    List<com.amazonaws.services.ecs.model.Service> services =
        awsClusterService.getServices(settingAttribute, getClusterName(context));
    Optional<com.amazonaws.services.ecs.model.Service> ecsService =
        services.stream().filter(svc -> svc.getServiceName().equals(ecsServiceName)).findFirst();
    if (!ecsService.isPresent()) {
      logger.info("Old ECS Service {} does not exist.. nothing to do", ecsServiceName);
      return -1;
    }

    int desiredCount = Math.max(ecsService.get().getDesiredCount() - instanceCount, 0);
    logger.info("Desired count for service {} is {}", ecsServiceName, desiredCount);
    return desiredCount;
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
