package software.wings.utils;

import java.io.Closeable;
import java.util.function.Function;

/**
 * Miscellaneous utility class.
 *
 * @author Rishi
 */
public class Misc {
  public interface ThrowingCallable { void run() throws Exception; }

  public interface ReturningThrowingCallable<T> { T run() throws Exception; }
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
   * Closes list of Closeables and swallows exception.
   *
   * @param closeables Closeable objects to close.
   */
  public static void quietClose(Closeable... closeables) {
    for (Closeable closeable : closeables) {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (Exception exception) {
        // Ignore
      }
    }
  }

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

  public static void ignoreException(ThrowingCallable callable) {
    try {
      callable.run();
    } catch (Exception e) {
      // Ignore
    }
  }

  public static <T> T ignoreException(ReturningThrowingCallable<T> callable, T defaultValue) {
    try {
      return callable.run();
    } catch (Exception e) {
      // Ignore
      return defaultValue;
    }
  }
}
