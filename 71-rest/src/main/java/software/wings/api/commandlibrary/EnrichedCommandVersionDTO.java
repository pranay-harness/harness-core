package software.wings.api.commandlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.commandlibrary.api.dto.CommandVersionDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "EnrichedCommandVersionDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichedCommandVersionDTO extends CommandVersionDTO {
  BaseTemplate templateObject;
  List<Variable> variables;
  String yamlContent;

  @Builder
  public EnrichedCommandVersionDTO(String commandName, String commandStoreName, String version, String description,
      Set<String> tags, String repoUrl, BaseTemplate templateObject, List<Variable> variables, String yamlContent) {
    super(commandName, commandStoreName, version, description, tags, repoUrl);
    this.templateObject = templateObject;
    this.variables = variables;
    this.yamlContent = yamlContent;
  }
}
