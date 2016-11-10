package software.wings.utils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by peeyushaggarwal on 6/16/16.
 */
public class ThreadContext {
  private static final AtomicReference<String> context = new AtomicReference<>("");

  /**
   * Clear context.
   */
  public static void clearContext() {
    context.set("");
  }

  /**
   * Gets context.
   *
   * @return the context
   */
  public static String getContext() {
    return context.get();
  }

  /**
   * Sets context.
   *
   * @param value the value
   */
  public static void setContext(String value) {
    context.set(value);
  }
}
