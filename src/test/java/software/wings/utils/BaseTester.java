package software.wings.utils;

import com.openpojo.validation.test.Tester;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public abstract class BaseTester implements Tester {
  public static boolean overridesMethod(Class<?> cls, String method) {
    return Misc.ignoreException(() -> cls.getMethod("toString").getDeclaringClass() == cls, false);
  }
}
