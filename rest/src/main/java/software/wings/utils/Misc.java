package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.common.Constants;
import software.wings.exception.WingsException;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    Matcher matcher = wildCharPattern.matcher(expression);
    return matcher.replaceAll(replacement);
  }

  /**
   * sleep without throwing InterruptedExeception.
   *
   * @param delay sleep interval in millis.
   */
  public static void quietSleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException exception) {
      // Ignore
    }
  }

  /**
   * Quiet sleep.
   *
   * @param delay the delay
   * @param unit  the unit
   */
  public static void quietSleep(int delay, TimeUnit unit) {
    quietSleep((int) unit.toMillis(delay));
  }

  /**
   * Sleep with runtime exception.
   *
   * @param delay the delay
   */
  public static void sleep(int delay, TimeUnit timeUnit) {
    try {
      Thread.sleep(timeUnit.toMillis(delay));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * As int.
   *
   * @param value the value
   * @return the int
   */
  public static int asInt(String value) {
    return asInt(value, 0);
  }

  /**
   * Converts a string to integer and in case of exception returns a default value.
   *
   * @param value        String to convert to int.
   * @param defaultValue defaultValue to return in case of exceptions.
   * @return converted int value or default.
   */
  public static int asInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception exception) {
      return defaultValue;
    }
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

  public static String trim(String key) {
    return key != null ? key.trim() : null;
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

  public static String getMessage(Throwable t) {
    if (t instanceof WingsException) {
      WingsException we = (WingsException) t;
      StringBuilder paramMsgBuilder = new StringBuilder();
      if (we.getParams() != null) {
        for (Object value : we.getParams().values()) {
          if (value != null) {
            paramMsgBuilder.append(value.toString()).append(". ");
          }
        }
      }
      String paramMsg = paramMsgBuilder.toString();
      if (isNotBlank(paramMsg)) {
        return t.getMessage() + " - " + paramMsg;
      } else {
        return "Failed with error " + t.getMessage();
      }
    } else {
      return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
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
    return str.replace(".", "\u2024");
  }

  /**
   * Replace "\u2024" with the dot char. Reverses the
   * replaceDotWithUnicode action above
   * @param str a string containing \u2024
   * @return replaced string
   */
  public static String replaceUnicodeWithDot(String str) {
    return str.replace("\u2024", ".");
  }
}
