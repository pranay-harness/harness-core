package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class HarnessUserThreadLocal {
  public static final ThreadLocal<HarnessUserAccountActions> values = new ThreadLocal<>();

  public static void set(HarnessUserAccountActions harnessUserAccountActions) {
    values.set(harnessUserAccountActions);
  }

  public static void unset() {
    values.remove();
  }

  public static HarnessUserAccountActions get() {
    return values.get();
  }
}
