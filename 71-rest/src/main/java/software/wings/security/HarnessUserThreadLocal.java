package software.wings.security;

import lombok.experimental.UtilityClass;

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
