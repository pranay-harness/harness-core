package software.wings.security;

/**
 * @author marklu on 2019-06-12
 */
public class HarnessUserThreadLocal {
  public static ThreadLocal<HarnessUserAccountActions> harnessUserThreadLocal = new ThreadLocal<>();

  public static void set(HarnessUserAccountActions harnessUserAccountActions) {
    harnessUserThreadLocal.set(harnessUserAccountActions);
  }

  public static void unset() {
    harnessUserThreadLocal.remove();
  }

  public static HarnessUserAccountActions get() {
    return harnessUserThreadLocal.get();
  }
}
