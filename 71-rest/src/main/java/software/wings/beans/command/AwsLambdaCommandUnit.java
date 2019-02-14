package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.DeploymentType;

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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("AWS_LAMBDA")
  public static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.AWS_LAMBDA.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.AWS_LAMBDA.name(), deploymentType);
    }
  }
}
