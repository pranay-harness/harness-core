package software.wings.utils;

import com.google.api.client.util.Throwables;

import com.codahale.metrics.Slf4jReporter.LoggingLevel;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import software.wings.common.Constants;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Miscellaneous utility class.
 *
 * @author Rishi
 */
public class Misc {
  private static Pattern wildCharPattern = Pattern.compile("[-|+|*|/|\\\\| |&|$|\"|'|\\.|\\|]");

  public static String normalizeExpression(String expression) {
    return normalizeExpression(expression, "__");
  }

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

  public static void quietSleep(int delay, TimeUnit unit) {
    quietSleep((int) unit.toMillis(delay));
  }

  public static void sleepWithRuntimeException(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException exception) {
      Throwables.propagate(exception);
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

  public static void error(Logger logger, String msg, Throwable t) {
    logger.error(msg, t);
    writeException(logger, LoggingLevel.ERROR, t);
  }

  public static void warn(Logger logger, String msg, Throwable t) {
    logger.warn(msg, t);
    writeException(logger, LoggingLevel.WARN, t);
  }

  public static void info(Logger logger, String msg, Throwable t) {
    logger.info(msg, t);
    writeException(logger, LoggingLevel.INFO, t);
  }

  public static void debug(Logger logger, String msg, Throwable t) {
    logger.debug(msg, t);
    writeException(logger, LoggingLevel.DEBUG, t);
  }

  private static void writeException(Logger logger, LoggingLevel level, Throwable t) {
    while (t != null) {
      logIt(logger, level,
          "***** Caused by: " + t.getClass().getCanonicalName()
              + (t.getMessage() != null ? ": " + t.getMessage() : ""));
      Arrays.stream(t.getStackTrace()).forEach(elem -> logIt(logger, level, " --- Trace: " + elem));
      t = t.getCause();
    }
  }

  private static void logIt(Logger logger, LoggingLevel level, String msg) {
    switch (level) {
      case ERROR:
        logger.error(msg);
        break;
      case WARN:
        logger.warn(msg);
        break;
      case DEBUG:
        logger.debug(msg);
        break;
      case INFO:
      default:
        logger.info(msg);
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
