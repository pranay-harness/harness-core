package io.harness.git;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.secret.SecretSanitizerThreadLocal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import lombok.experimental.UtilityClass;

/***
 * This code doesn't sanitizes any generic exception or error messages
 *
 * It uses the secret value listed in the SecretSanitizerThreadLocal variable
 * to sanitize the messages
 */

@UtilityClass
public class ExceptionSanitizer {
  public static String SECRET_MASK = "#######";

  public String sanitizeForLogging(Throwable ex) {
    if (ex == null) {
      return null;
    }
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    String exceptionAsString = sw.toString();
    return sanitizeTheMessage(exceptionAsString);
  }

  public String sanitizeTheMessage(String message) {
    Set<String> secrets = SecretSanitizerThreadLocal.get();
    if (isEmpty(secrets)) {
      return message;
    }
    String updatedMessage = message;
    for (String secret : secrets) {
      updatedMessage = updatedMessage.replace(secret, SECRET_MASK);
    }
    return updatedMessage;
  }
}
