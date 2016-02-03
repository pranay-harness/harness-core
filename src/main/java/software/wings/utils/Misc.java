package software.wings.utils;

import java.io.Closeable;

/**
 *  Miscellaneous utility class.
 *
 *
 * @author Rishi
 *
 */
public class Misc {
  public static void quietSleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
    }
  }

  public static void quietClose(Closeable... Closeable) {
    for (Closeable c : Closeable) {
      try {
        if (c != null)
          c.close();
      } catch (Exception e) {
      }
    }
  }

  public static int asInt(String value) {
    return asInt(value, 0);
  }
  public static int asInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
