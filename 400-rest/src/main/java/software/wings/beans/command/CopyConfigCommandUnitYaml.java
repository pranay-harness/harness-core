package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("COPY_CONFIGS")
public class CopyConfigCommandUnitYaml extends AbstractCommandUnitYaml {
  private String destinationParentPath;

  public CopyConfigCommandUnitYaml() {
    super(CommandUnitType.COPY_CONFIGS.name());
  }

  @Builder
  public CopyConfigCommandUnitYaml(String name, String deploymentType, String destinationParentPath) {
    super(name, CommandUnitType.COPY_CONFIGS.name(), deploymentType);
    this.destinationParentPath = destinationParentPath;
  }
}
