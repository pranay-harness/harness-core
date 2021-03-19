package software.wings.beans.command;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CODE_DEPLOY")
public class CodeDeployCommandUnitYaml extends AbstractCommandUnitYaml {
  public CodeDeployCommandUnitYaml() {
    super(CommandUnitType.CODE_DEPLOY.name());
  }

  @Builder
  public CodeDeployCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.CODE_DEPLOY.name(), deploymentType);
  }
}
