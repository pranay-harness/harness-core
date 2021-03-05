package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
