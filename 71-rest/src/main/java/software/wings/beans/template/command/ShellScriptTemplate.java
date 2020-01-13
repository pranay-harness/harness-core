package software.wings.beans.template.command;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.template.BaseTemplate;

@JsonTypeName(SHELL_SCRIPT)
@Value
@Builder
@JsonInclude(NON_NULL)
public class ShellScriptTemplate implements BaseTemplate {
  private String scriptType;
  private String scriptString;
  private String outputVars;
  @Builder.Default private int timeoutMillis = 600000;
}
