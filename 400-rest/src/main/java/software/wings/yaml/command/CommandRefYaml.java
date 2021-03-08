package software.wings.yaml.command;

import software.wings.beans.command.AbstractCommandUnitYaml;
import software.wings.beans.command.CommandUnitType;
import software.wings.yaml.templatelibrary.TemplateLibraryYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This yaml is used to represent a command reference. A command could be referred from another command, in that case,
 * we need a ref.
 *
 * @author rktummala on 11/16/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("COMMAND")
public class CommandRefYaml extends AbstractCommandUnitYaml {
  private List<TemplateLibraryYaml.TemplateVariableYaml> variables;
  private String templateUri;

  public CommandRefYaml() {
    super(CommandUnitType.COMMAND.name());
  }

  @Builder
  public CommandRefYaml(String name, String deploymentType, String templateUri,
      List<TemplateLibraryYaml.TemplateVariableYaml> variables) {
    super(name, CommandUnitType.COMMAND.name(), deploymentType);
    setVariables(variables);
    setTemplateUri(templateUri);
  }
}
