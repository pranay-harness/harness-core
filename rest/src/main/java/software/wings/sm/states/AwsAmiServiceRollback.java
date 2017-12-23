package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceRollback extends AwsAmiServiceDeployState {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Amazon AMI")
  private String commandName = "Amazon AMI";

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public AwsAmiServiceRollback(String name) {
    super(name, StateType.AWS_AMI_SERVICE_ROLLBACK.name());
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  @Override
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
