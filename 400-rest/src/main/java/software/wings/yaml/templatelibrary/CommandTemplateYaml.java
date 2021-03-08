package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.SSH;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.command.AbstractCommandUnitYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(SSH)
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(Module._870_CG_YAML_BEANS)
@BreakDependencyOn("software.wings.yaml.templatelibrary.CommandTemplateYaml")
public class CommandTemplateYaml extends TemplateLibraryYaml {
  private String commandUnitType;
  private List<AbstractCommandUnitYaml> commandUnits = new ArrayList<>();

  @Builder
  public CommandTemplateYaml(String type, String harnessApiVersion, String description,
      List<TemplateVariableYaml> templateVariableYamlList, String commandUnitType,
      List<AbstractCommandUnitYaml> commandUnits) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.commandUnitType = commandUnitType;
    this.commandUnits = commandUnits;
  }
}
