package software.wings.delegatetasks;

import static io.harness.expression.SecretString.SECRET_MASK;
import static org.apache.commons.lang3.StringUtils.replaceEach;

import java.util.ArrayList;
import java.util.Set;

public abstract class LogSanitizer {
  public abstract String sanitizeLog(String activityId, String log);

  protected String sanitizeLogInternal(String log, Set<String> secrets) {
    ArrayList<String> secretMasks = new ArrayList<>();
    ArrayList<String> secretValues = new ArrayList<>();
    for (String secret : secrets) {
      secretMasks.add(SECRET_MASK);
      secretValues.add(secret);
    }
    return replaceEach(log, secretValues.toArray(new String[] {}), secretMasks.toArray(new String[] {}));
  }
}
