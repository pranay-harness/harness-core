package software.wings.expression;

import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShellScriptEnvironmentVariables {
  @Getter private final Map<String, String> outputVars;
  @Getter private final Map<String, String> secretOutputVars;

  public ShellScriptEnvironmentVariables(Map<String, String> outputVars, Map<String, String> secretOutputVars) {
    this.outputVars = outputVars;
    this.secretOutputVars = secretOutputVars;
  }

  public Object get(Object key) {
    if (outputVars.containsKey(key)) {
      return outputVars.get(key);
    }
    if (secretOutputVars.containsKey(key)) {
      String value = secretOutputVars.get(key);
      String keyString = (String) key;
      return "${sweepingOutputSecrets.obtain(\"" + keyString + "\",\"" + value + "\")}";
    }
    return null;
  }
}
