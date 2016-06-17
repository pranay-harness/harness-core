package software.wings.utils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by peeyushaggarwal on 6/16/16.
 */
public class ThreadContext {
  private static final AtomicReference<String> context = new AtomicReference<>("");

  public static void clearContext() {
    context.set("");
  }

  public static String getContext() {
    return context.get();
  }

  public static void setContext(String value) {
    context.set(value);
  }
}
