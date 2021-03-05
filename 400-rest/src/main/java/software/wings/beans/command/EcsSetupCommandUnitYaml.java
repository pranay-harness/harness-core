package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
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
