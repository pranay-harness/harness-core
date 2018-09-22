package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * Miscellaneous utility class.
 *
 * @author Rishi
 */
public class Misc {
  private static final Pattern wildCharPattern = Pattern.compile("[-|+|*|/|\\\\| |&|$|\"|'|\\.|\\|]");
  public static final Pattern commaCharPattern = Pattern.compile("\\s*,\\s*");
  public static final int MAX_CAUSES = 10;

  /**
   * Normalize expression string.
   *
   * @param expression the expression
   * @return the string
   */
  public static String normalizeExpression(String expression) {
    return normalizeExpression(expression, "__");
  }

  /**
   * Normalize expression string.
   *
   * @param expression  the expression
   * @param replacement the replacement
   * @return the string
   */
  public static String normalizeExpression(String expression, String replacement) {
    Matcher matcher = wildCharPattern.matcher(trim(expression));
    return matcher.replaceAll(replacement);
  }

  /**
   * Ignore exception.
   *
   * @param callable the callable
   */
  public static void ignoreException(ThrowingCallable callable) {
    try {
      callable.run();
    } catch (Exception e) {
      // Ignore
    }
  }

  /**
   * Ignore exception.
   *
   * @param <T>          the generic type
   * @param callable     the callable
   * @param defaultValue the default value
   * @return the t
   */
  public static <T> T ignoreException(ReturningThrowingCallable<T> callable, T defaultValue) {
    try {
      return callable.run();
    } catch (Exception e) {
      // Ignore
      return defaultValue;
    }
  }

  /**
   * Checks if is wild char present.
   *
   * @param names the names
   * @return true, if is wild char present
   */
  public static boolean isWildCharPresent(String... names) {
    if (isEmpty(names)) {
      return false;
    }
    for (String name : names) {
      if (name.indexOf(Constants.WILD_CHAR) >= 0) {
        return true;
      }
    }
    return false;
  }

  public static void logAllMessages(Exception ex, ExecutionLogCallback executionLogCallback) {
    int i = 0;
    Throwable t = ex;
    while (t != null && i++ < MAX_CAUSES) {
      String msg = getMessage(t);
      if (isNotBlank(msg)) {
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      }
      t = t.getCause();
    }
  }

  public static void logAllMessages(
      Throwable ex, ManagerExecutionLogCallback executionLogCallback, CommandExecutionStatus commandExecutionStatus) {
    int i = 0;
    Throwable t = ex;
    while (t != null && i++ < MAX_CAUSES) {
      String msg = getMessage(t);
      if (isNotBlank(msg)) {
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR, commandExecutionStatus);
      }
      t = t.getCause();
    }
  }

  public static String getMessage(Throwable t) {
    if (t instanceof WingsException) {
      WingsException we = (WingsException) t;
      return WingsExceptionMapper.getResponseMessageList(we, REST_API)
          .stream()
          .map(ResponseMessage::getMessage)
          .collect(joining(". "));
    } else if (t instanceof ConstraintViolationException) {
      ConstraintViolationException constraintViolationException = (ConstraintViolationException) t;
      return constraintViolationException.getConstraintViolations()
          .stream()
          .map(ConstraintViolation::getMessage)
          .collect(joining(". "));
    } else if (t instanceof HarnessException) {
      HarnessException he = (HarnessException) t;
      Throwable cause = he.getCause();
      if (cause instanceof WingsException) {
        WingsException we = (WingsException) cause;
        return WingsExceptionMapper.getResponseMessageList(we, REST_API)
            .stream()
            .map(ResponseMessage::getMessage)
            .collect(joining(". "));
      } else {
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
      }
    } else {
      return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }
  }

  public static ServiceApiVersion parseApisVersion(String acceptHeader) {
    if (StringUtils.isEmpty(acceptHeader)) {
      return null;
    }

    String[] headers = acceptHeader.split(",");
    String header = headers[0].trim();
    if (!header.startsWith("application/")) {
      throw new IllegalArgumentException("Invalid header " + acceptHeader);
    }

    String versionHeader = header.replace("application/", "").trim();
    if (StringUtils.isEmpty(versionHeader)) {
      throw new IllegalArgumentException("Invalid header " + acceptHeader);
    }

    String[] versionSplit = versionHeader.split("\\+");

    String version = versionSplit[0].trim();
    if (version.toUpperCase().charAt(0) == 'V') {
      return ServiceApiVersion.valueOf(version.toUpperCase());
    }

    return ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
  }

  public static String getDurationString(long start, long end) {
    return getDurationString(end - start);
  }

  public static String getDurationString(long duration) {
    long elapsedHours = duration / TimeUnit.HOURS.toMillis(1);
    duration = duration % TimeUnit.HOURS.toMillis(1);

    long elapsedMinutes = duration / TimeUnit.MINUTES.toMillis(1);
    duration = duration % TimeUnit.MINUTES.toMillis(1);

    long elapsedSeconds = duration / TimeUnit.SECONDS.toMillis(1);

    StringBuilder elapsed = new StringBuilder();

    if (elapsedHours > 0) {
      elapsed.append(elapsedHours).append('h');
    }
    if (elapsedMinutes > 0) {
      if (isNotEmpty(elapsed.toString())) {
        elapsed.append(' ');
      }
      elapsed.append(elapsedMinutes).append('m');
    }
    if (elapsedSeconds > 0) {
      if (isNotEmpty(elapsed.toString())) {
        elapsed.append(' ');
      }
      elapsed.append(elapsedSeconds).append('s');
    }

    if (isEmpty(elapsed.toString())) {
      elapsed.append("0s");
    }

    return elapsed.toString();
  }

  /**
   * The Interface ThrowingCallable.
   */
  public interface ThrowingCallable {
    /**
     * Run.
     *
     * @throws Exception the exception
     */
    void run() throws Exception;
  }

  /**
   * The Interface ReturningThrowingCallable.
   *
   * @param <T> the generic type
   */
  public interface ReturningThrowingCallable<T> {
    /**
     * Run.
     *
     * @return the t
     * @throws Exception the exception
     */
    T run() throws Exception;
  }

  /**
   * Replace dot with the unicode value so
   * the string can be used as Map keys in
   * mongo.
   * @param str a string containing dots
   * @return replaced string
   */
  public static String replaceDotWithUnicode(String str) {
    return str.replaceAll("\\.", "\u2024");
  }

  /**
   * Replace "\u2024" with the dot char. Reverses the
   * replaceDotWithUnicode action above
   * @param str a string containing \u2024
   * @return replaced string
   */
  public static String replaceUnicodeWithDot(String str) {
    return str.replaceAll("\u2024", ".");
  }

  public static String generateSecretKey() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, e);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    return Hex.encodeHexString(encoded);
  }
}
