package software.wings.graphql.datafetcher;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PrincipalThreadLocal {
  public static final ThreadLocal<Principal> triggeredByThreadLocal = new ThreadLocal<>();

  /**
   *
   * @param principal
   */
  public static void set(final Principal principal) {
    triggeredByThreadLocal.set(principal);
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
  public static Principal get() {
    return triggeredByThreadLocal.get();
  }
}
