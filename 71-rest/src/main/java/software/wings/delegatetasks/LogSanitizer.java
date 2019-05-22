package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang3.StringUtils.replaceEach;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Replace secret values with secret name expressions for safe display
 */
@Slf4j
public class LogSanitizer {
  private final String activityId;
  // Map of secret names to unmasked secret values
  private final Map<String, String> secrets;

  public LogSanitizer(String activityId, Map<String, String> secrets) {
    this.activityId = activityId;
    this.secrets = secrets;
  }

  /**
   * Replace secret values in {@code log} with secret name expressions for safe display
   * @param activityId The id to match to the set of secrets for this sanitizer
   * @param log The text that may contain secret values
   * @return text with secrets replaced by a secret name expression
   */
  public String sanitizeLog(String activityId, String log) {
    if (StringUtils.equals(activityId, this.activityId)) {
      if (isEmpty(secrets)) {
        return log;
      }
      ArrayList<String> secretNames = new ArrayList<>();
      ArrayList<String> secretValues = new ArrayList<>();
      for (Map.Entry<String, String> entry : secrets.entrySet()) {
        secretNames.add("${" + entry.getKey() + "}");
        secretValues.add(entry.getValue());
      }
      return replaceEach(log, secretValues.toArray(new String[] {}), secretNames.toArray(new String[] {}));
    }
    return log;
  }
}
