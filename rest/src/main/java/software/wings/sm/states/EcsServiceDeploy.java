package software.wings.sm.states;

import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;

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

  @Override
  protected ContainerResizeParams buildContainerResizeParams(
      ContextData contextData, List<ContainerServiceData> desiredCounts) {
    return anEcsResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withDesiredCounts(desiredCounts)
        .withRegion(contextData.region)
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .build();
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
