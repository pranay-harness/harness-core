package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.DeploymentType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

/**
 * Created by anubhaw on 12/20/17.
 */
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
  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("AWS_AMI")
  public static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.AWS_AMI.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.AWS_AMI.name(), deploymentType);
    }
  }
}
