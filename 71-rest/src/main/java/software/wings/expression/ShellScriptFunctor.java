package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.expression.ExpressionFunctor;
import io.harness.expression.LateBindingValue;
import io.harness.expression.SecretString;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Builder
@Slf4j
public class ShellScriptFunctor implements ExpressionFunctor {
  private ScriptType scriptType;

  public String escape(SecretString input) {
    return escape(input.toString());
  }

  public String escape(LateBindingValue input) {
    return escape(input.bind().toString());
  }

  public String escape(String input) {
    if (ScriptType.BASH == scriptType) {
      return input.replace("'", "\\'")
          .replace("`", "\\`")
          .replace("$", "\\$")
          .replace("&", "\\&")
          .replace("(", "\\(")
          .replace(")", "\\)")
          .replace("|", "\\|")
          .replace(";", "\\;")
          .replace("\"", "\\\"");
    } else if (ScriptType.POWERSHELL == scriptType) {
      return "\"" + input.replace("\"", "`\"") + "\"";
    }
    return input;
  }
}
