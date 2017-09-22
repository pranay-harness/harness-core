package software.wings.beans.command;

import software.wings.api.DeploymentType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

public class AwsLambdaCommandUnit extends AbstractCommandUnit {
  public AwsLambdaCommandUnit() {
    super(CommandUnitType.AWS_LAMBDA);
    setArtifactNeeded(true);
    setDeploymentType(DeploymentType.AWS_LAMBDA.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return null;
  }
}
