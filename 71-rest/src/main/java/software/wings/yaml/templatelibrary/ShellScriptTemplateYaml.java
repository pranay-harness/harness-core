package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(SHELL_SCRIPT)
@JsonPropertyOrder({"harnessApiVersion"})
public class ShellScriptTemplateYaml extends TemplateLibraryYaml {
  private String scriptType;
  private String scriptString;
  private String outputVars;
  private int timeoutMillis = 600000;

  @Builder
  public ShellScriptTemplateYaml(String type, String harnessApiVersion, String description, String scriptType,
      String scriptString, String outputVars, int timeOutMillis, List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.outputVars = outputVars;
    this.scriptString = scriptString;
    this.scriptType = scriptType;
    this.timeoutMillis = timeOutMillis;
  }
}