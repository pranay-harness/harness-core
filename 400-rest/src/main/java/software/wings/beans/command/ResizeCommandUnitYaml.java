package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("RESIZE")
public class ResizeCommandUnitYaml extends ContainerResizeCommandUnitYaml {
  public ResizeCommandUnitYaml() {
    super(CommandUnitType.RESIZE.name());
  }

  @Builder
  public ResizeCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.RESIZE.name(), deploymentType);
  }
}
