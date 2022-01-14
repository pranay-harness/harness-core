package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
