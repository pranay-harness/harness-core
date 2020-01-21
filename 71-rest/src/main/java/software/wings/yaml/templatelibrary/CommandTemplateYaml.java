package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.SSH;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.command.AbstractCommandUnit;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(SSH)
@JsonPropertyOrder({"harnessApiVersion"})
public class CommandTemplateYaml extends TemplateLibraryYaml {
  private String commandUnitType;
  private List<AbstractCommandUnit.Yaml> commandUnits = new ArrayList<>();

  @Builder
  public CommandTemplateYaml(String type, String harnessApiVersion, String description,
      List<TemplateVariableYaml> templateVariableYamlList, String commandUnitType,
      List<AbstractCommandUnit.Yaml> commandUnits) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.commandUnitType = commandUnitType;
    this.commandUnits = commandUnits;
  }
}
