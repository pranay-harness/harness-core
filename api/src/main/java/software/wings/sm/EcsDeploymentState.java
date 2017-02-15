package software.wings.sm;

/**
 * Created by rishi on 2/15/17.
 */
public class EcsDeploymentState implements PhaseStepExecutionState {
  private String ecsServiceName;
  private boolean newEcsService;
  private String loadBalancerSettingId;
  private String commandName;
  private int instanceCount;

  public String getEcsServiceName() {
    return ecsServiceName;
  }

  public void setEcsServiceName(String ecsServiceName) {
    this.ecsServiceName = ecsServiceName;
  }

  public boolean isNewEcsService() {
    return newEcsService;
  }

  public void setNewEcsService(boolean newEcsService) {
    this.newEcsService = newEcsService;
  }

  public String getLoadBalancerSettingId() {
    return loadBalancerSettingId;
  }

  public void setLoadBalancerSettingId(String loadBalancerSettingId) {
    this.loadBalancerSettingId = loadBalancerSettingId;
  }

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

  public static final class EcsDeploymentStateBuilder {
    private String ecsServiceName;
    private boolean newEcsService;
    private String loadBalancerSettingId;
    private String commandName;
    private int instanceCount;

    private EcsDeploymentStateBuilder() {}

    public static EcsDeploymentStateBuilder anEcsDeploymentState() {
      return new EcsDeploymentStateBuilder();
    }

    public EcsDeploymentStateBuilder withEcsServiceName(String ecsServiceName) {
      this.ecsServiceName = ecsServiceName;
      return this;
    }

    public EcsDeploymentStateBuilder withNewEcsService(boolean newEcsService) {
      this.newEcsService = newEcsService;
      return this;
    }

    public EcsDeploymentStateBuilder withLoadBalancerSettingId(String loadBalancerSettingId) {
      this.loadBalancerSettingId = loadBalancerSettingId;
      return this;
    }

    public EcsDeploymentStateBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public EcsDeploymentStateBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public EcsDeploymentState build() {
      EcsDeploymentState ecsDeploymentState = new EcsDeploymentState();
      ecsDeploymentState.setEcsServiceName(ecsServiceName);
      ecsDeploymentState.setNewEcsService(newEcsService);
      ecsDeploymentState.setLoadBalancerSettingId(loadBalancerSettingId);
      ecsDeploymentState.setCommandName(commandName);
      ecsDeploymentState.setInstanceCount(instanceCount);
      return ecsDeploymentState;
    }
  }
}
