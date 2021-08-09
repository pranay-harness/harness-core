package software.wings.beans.template.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.BaseTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@JsonTypeName(SHELL_SCRIPT)
@Value
@Builder
@JsonInclude(NON_NULL)
public class ShellScriptTemplate implements BaseTemplate {
  private String scriptType;
  private String scriptString;
  private String outputVars;
  private String secretOutputVars;
  @Builder.Default private int timeoutMillis = 600000;
}
