package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * Replace secret values with mask for safe display
 */
@Slf4j
public class ActivityBasedLogSanitizer extends LogSanitizer {
  private final String activityId;
  private final Set<String> secrets;

  public ActivityBasedLogSanitizer(String activityId, Set<String> secrets) {
    this.activityId = activityId;
    this.secrets = secrets;
  }

  /**
   * Replace secret values in {@code log} with mask for safe display
   * @param activityId The id to match to the set of secrets for this sanitizer
   * @param log The text that may contain secret values
   * @return text with secrets replaced by a mask
   */
  @Override
  public String sanitizeLog(String activityId, String log) {
    if (StringUtils.equals(activityId, this.activityId)) {
      if (isEmpty(secrets)) {
        return log;
      }
      return sanitizeLogInternal(log, secrets);
    }
    return log;
  }
}
