package software.wings.api.commandlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.commandlibrary.api.dto.CommandVersionDTO;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;

import java.beans.ConstructorProperties;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "EnrichedCommandVersionDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichedCommandVersionDTO extends CommandVersionDTO {
  BaseTemplate templateObject;
  List<Variable> variables;

  @Builder
  @ConstructorProperties(
      {"commandId", "commandStoreId", "version", "description", "yamlContent", "templateObject", "variables"})
  public EnrichedCommandVersionDTO(String commandId, String commandStoreId, String version, String description,
      String yamlContent, BaseTemplate templateObject, List<Variable> variables) {
    super(commandId, commandStoreId, version, description, yamlContent);
    this.templateObject = templateObject;
    this.variables = variables;
  }
}
