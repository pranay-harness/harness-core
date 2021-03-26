package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ECS_SETUP")
public class EcsSetupCommandUnitYaml extends ContainerSetupCommandUnitYaml {
  public EcsSetupCommandUnitYaml() {
    super(CommandUnitType.ECS_SETUP.name());
  }

  @Builder
  public EcsSetupCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.ECS_SETUP.name(), deploymentType);
  }
}
