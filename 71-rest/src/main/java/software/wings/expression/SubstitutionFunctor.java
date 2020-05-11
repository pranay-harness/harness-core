package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.LateBindingMap;
import lombok.Builder;

@OwnedBy(CDC)
@Builder
public class SubstitutionFunctor extends LateBindingMap {
  public Object startTailLogVerification(String... args) {
    return substitute("harness_utils_start_tail_log_verification", args);
  }

  public Object waitForTailLogVerification(String... args) {
    return substitute("harness_utils_wait_for_tail_log_verification", args);
  }

  private String substitute(String functionName, String... args) {
    StringBuilder output = new StringBuilder(functionName);
    for (String arg : args) {
      output.append(" \"").append(arg).append('"');
    }
    return output.toString();
  }

  @Override
  public Object get(Object key) {
    throw new UnsupportedOperationException("Not supported");
  }
}
