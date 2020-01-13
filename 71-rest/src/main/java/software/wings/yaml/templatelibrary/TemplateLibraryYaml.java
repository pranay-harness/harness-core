package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;

/**
 * @author abhinav
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = ShellScriptTemplateYaml.class, name = SHELL_SCRIPT) })
public abstract class TemplateLibraryYaml extends BaseEntityYaml {
  private String description;
  private List<TemplateVariableYaml> variables;

  public TemplateLibraryYaml(
      String type, String harnessApiVersion, String description, List<TemplateVariableYaml> variables) {
    super(type, harnessApiVersion);
    this.description = description;
    this.variables = variables;
  }

  @Data
  @Builder
  public static class TemplateVariableYaml {
    String name;
    String description;
    String value;
  }
}
