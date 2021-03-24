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
@JsonTypeName("RESIZE")
public class ResizeCommandUnitYaml extends CommandExecutionStatusYaml {
  public ResizeCommandUnitYaml() {
    super(CommandUnitType.RESIZE.name());
  }

  @Builder
  public ResizeCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.RESIZE.name(), deploymentType);
  }
}
