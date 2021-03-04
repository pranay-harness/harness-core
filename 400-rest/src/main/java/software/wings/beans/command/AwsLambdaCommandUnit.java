package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

import software.wings.api.DeploymentType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("AWS_LAMBDA")
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
