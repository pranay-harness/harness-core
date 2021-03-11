package software.wings.beans.command;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("AWS_AMI")
public class AmiCommandUnitYaml extends AbstractCommandUnitYaml {
  public AmiCommandUnitYaml() {
    super(CommandUnitType.AWS_AMI.name());
  }

  @lombok.Builder
  public AmiCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.AWS_AMI.name(), deploymentType);
  }
}
