package software.wings.delegatetasks;

import static io.harness.expression.SecretString.SECRET_MASK;

import static org.apache.commons.lang3.StringUtils.replaceEach;

import io.harness.data.structure.HasPredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class LogSanitizer {
  public static Set<String> calculateSecretLines(Set<String> secrets) {
    return secrets.stream()
        .flatMap(secret -> {
          String[] split = secret.split("\\r?\\n");
          return Arrays.stream(split).map(ActivityBasedLogSanitizer::cleanup).filter(HasPredicate::hasSome);
        })
        .collect(Collectors.toSet());
  }

  public static String cleanup(String secretLine) {
    if (secretLine == null) {
      return null;
    }
    String line = secretLine.trim();
    if (line.length() < 3) {
      return null;
    }
    return line;
  }

  public abstract String sanitizeLog(String activityId, String message);

  protected String sanitizeLogInternal(String message, Set<String> secrets) {
    ArrayList<String> secretMasks = new ArrayList<>();
    ArrayList<String> secretValues = new ArrayList<>();
    for (String secret : secrets) {
      secretMasks.add(SECRET_MASK);
      secretValues.add(secret);
    }
    return replaceEach(message, secretValues.toArray(new String[] {}), secretMasks.toArray(new String[] {}));
  }
}
