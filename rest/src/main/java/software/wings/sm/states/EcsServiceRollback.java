package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.sm.ContextElementType;
import software.wings.sm.StateType;
import software.wings.stencils.EnumData;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Created by brett on 3/24/17
 */
public class EcsServiceRollback extends ContainerServiceDeploy {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  private String commandName = "Resize Service Cluster";

  @Inject @Transient private transient AwsClusterService awsClusterService;

  public EcsServiceRollback(String name) {
    super(name, StateType.ECS_SERVICE_ROLLBACK.name());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String clusterName, @Nullable String serviceName) {
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
  public int fetchDesiredCount() {
    return 0;
  }

  public static final class EcsServiceRollbackBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private String commandName;

    private EcsServiceRollbackBuilder(String name) {
      this.name = name;
    }

    public static EcsServiceRollbackBuilder anEcsServiceRollback(String name) {
      return new EcsServiceRollbackBuilder(name);
    }

    public EcsServiceRollbackBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public EcsServiceRollbackBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public EcsServiceRollbackBuilder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public EcsServiceRollbackBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public EcsServiceRollbackBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public EcsServiceRollback build() {
      EcsServiceRollback ecsServiceRollback = new EcsServiceRollback(name);
      ecsServiceRollback.setId(id);
      ecsServiceRollback.setRequiredContextElementType(requiredContextElementType);
      ecsServiceRollback.setStateType(stateType);
      ecsServiceRollback.setRollback(true);
      ecsServiceRollback.setCommandName(commandName);
      return ecsServiceRollback;
    }
  }
}
