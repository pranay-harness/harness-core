package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
