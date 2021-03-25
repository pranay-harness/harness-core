package software.wings.delegatetasks;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Replace secret values with mask for safe display
 */
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class GenericLogSanitizer extends LogSanitizer {
  private final Set<String> secretLines;

  public GenericLogSanitizer(Set<String> secrets) {
    secretLines = calculateSecretLines(secrets);
  }

  /**
   * Replace secret values in {@code log} with mask for safe display (Done for all lines coming here)
   * @param activityId This activity is ignored
   * @param message The text that may contain secret values
   * @return text with secrets replaced by a mask
   */
  @Override
  public String sanitizeLog(String activityId, String message) {
    if (hasNone(secretLines)) {
      return message;
    }
    return sanitizeLogInternal(message, secretLines);
  }
}
