package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import software.wings.beans.InstanceUnitType;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceDeployState extends State {
  @Attributes(title = "Desired Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Amazon AMI")
  private String commandName;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsAmiServiceDeployState(String name) {
    super(name, StateType.AWS_AMI_SERVICE_DEPLOY.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ContextElement contextElement = context.getContextElement(ContextElementType.AMI_SERVICE);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.FAILED)
        .withErrorMessage("Not Implemented")
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets instance count.
   *
   * @return the instance count
   */
  public int getInstanceCount() {
    return instanceCount;
  }

  /**
   * Sets instance count.
   *
   * @param instanceCount the instance count
   */
  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  /**
   * Gets instance unit type.
   *
   * @return the instance unit type
   */
  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  /**
   * Sets instance unit type.
   *
   * @param instanceUnitType the instance unit type
   */
  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
