package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by brett on 6/22/17
 */
public class AwsCodeDeployDeploy extends State {
  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("CodeDeploy Install")
  private String commandName = "CodeDeploy Install";

  public AwsCodeDeployDeploy(String name) {
    super(name, StateType.AWS_CD_DEPLOY.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return anExecutionResponse().withExecutionStatus(ExecutionStatus.SUCCESS).build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
