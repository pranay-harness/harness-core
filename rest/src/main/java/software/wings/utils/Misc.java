package software.wings.utils;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.google.api.client.util.Throwables;

import com.codahale.metrics.Slf4jReporter.LoggingLevel;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import software.wings.common.Constants;

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

  /**
   * Normalize expression string.
   *
   * @param expression the expression
   * @return the string
   */
  static String normalizeExpression(String expression) {
    return normalizeExpression(expression, "__");
  }

  /**
   * Normalize expression string.
   *
   * @param expression  the expression
   * @param replacement the replacement
   * @return the string
   */
  static String normalizeExpression(String expression, String replacement) {
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
  public static void sleepWithRuntimeException(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException exception) {
      Throwables.propagate(exception);
    }
  }

  /**
   * Is null or empty boolean.
   *
   * @param str the str
   * @return the boolean
   */
  public static boolean isNullOrEmpty(String str) {
    return str == null || str.length() == 0;
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
   * Error.
   *
   * @param logger the logger
   * @param msg    the msg
   * @param t      the t
   */
  public static void error(Logger logger, String msg, Throwable t) {
    writeException(logger, LoggingLevel.ERROR, msg, t);
  }

  /**
   * Warn.
   *
   * @param logger the logger
   * @param msg    the msg
   * @param t      the t
   */
  public static void warn(Logger logger, String msg, Throwable t) {
    writeException(logger, LoggingLevel.WARN, msg, t);
  }

  /**
   * Info.
   *
   * @param logger the logger
   * @param msg    the msg
   * @param t      the t
   */
  public static void info(Logger logger, String msg, Throwable t) {
    writeException(logger, LoggingLevel.INFO, msg, t);
  }

  /**
   * Debug.
   *
   * @param logger the logger
   * @param msg    the msg
   * @param t      the t
   */
  public static void debug(Logger logger, String msg, Throwable t) {
    writeException(logger, LoggingLevel.DEBUG, msg, t);
  }

  private static void writeException(Logger logger, LoggingLevel level, String msg, Throwable t) {
    logIt(logger, level, isNotEmpty(msg) ? msg : "An exception occurred: " + t.getClass().getSimpleName(), t);
  }

  private static void logIt(Logger logger, LoggingLevel level, String msg, Throwable t) {
    switch (level) {
      case ERROR:
        logger.error(msg, t);
        break;
      case WARN:
        logger.warn(msg, t);
        break;
      case DEBUG:
        logger.debug(msg, t);
        break;
      case INFO:
      default:
        logger.info(msg, t);
    }
  }

  /**
   * Checks if is wild char present.
   *
   * @param names the names
   * @return true, if is wild char present
   */
  public static boolean isWildCharPresent(String... names) {
    if (ArrayUtils.isEmpty(names)) {
      return false;
    }
    for (String name : names) {
      if (name.indexOf(Constants.WILD_CHAR) >= 0) {
        return true;
      }
    }
    return false;
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
}
