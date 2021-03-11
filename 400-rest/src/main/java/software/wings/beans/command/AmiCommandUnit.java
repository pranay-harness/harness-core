package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

import software.wings.api.DeploymentType;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Created by anubhaw on 12/20/17.
 */

@JsonTypeName("AWS_AMI")
public class AmiCommandUnit extends AbstractCommandUnit {
  public AmiCommandUnit() {
    super(CommandUnitType.AWS_AMI);
    setArtifactNeeded(true);
    setDeploymentType(DeploymentType.AMI.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return null;
  }
}
