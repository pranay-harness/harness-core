package software.wings.expression;

import io.harness.delegate.task.shell.ScriptType;
import io.harness.expression.ExpressionFunctor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
public class ShellScriptFunctor implements ExpressionFunctor {
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptFunctor.class);
  private ScriptType scriptType;

  public String escapeString(String input) {
    if (ScriptType.BASH.equals(scriptType)) {
      return input.replace("'", "\\'")
          .replace("`", "\\`")
          .replace("$", "\\$")
          .replace("&", "\\&")
          .replace("(", "\\(")
          .replace(")", "\\)")
          .replace("|", "\\|")
          .replace(";", "\\;")
          .replace("\"", "\\\"");
    } else if (ScriptType.POWERSHELL.equals(scriptType)) {
      return "\"" + input.replace("\"", "`\"") + "\"";
    }
    return input;
  }
}
