package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("AWS_LAMBDA")
public class AwsLambdaCommandUnitYaml extends AbstractCommandUnitYaml {
  public AwsLambdaCommandUnitYaml() {
    super(CommandUnitType.AWS_LAMBDA.name());
  }

  @lombok.Builder
  public AwsLambdaCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.AWS_LAMBDA.name(), deploymentType);
  }
}
