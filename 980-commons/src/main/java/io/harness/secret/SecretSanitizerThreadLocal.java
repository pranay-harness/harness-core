package io.harness.secret;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretSanitizerThreadLocal {
  public static final ThreadLocal<Set<String>> triggeredByThreadLocal = new ThreadLocal<>();

  /**
   *
   * @param secrets
   */
  public static void set(final Set<String> secrets) {
    triggeredByThreadLocal.set(secrets);
  }

  /**
   * Unset.
   */
  public static void unset() {
    triggeredByThreadLocal.remove();
  }

  /**
   *
   * @return
   */
  public static Set<String> get() {
    return triggeredByThreadLocal.get();
  }
}