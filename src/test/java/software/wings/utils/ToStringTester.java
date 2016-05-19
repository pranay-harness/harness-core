package software.wings.utils;

import com.openpojo.reflection.PojoClass;
import com.openpojo.validation.test.Tester;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public class ToStringTester extends BaseTester {
  @Override
  public void run(PojoClass pojoClass) {
    if (overridesMethod(pojoClass.getClazz(), "toString")) {
      Misc.ignoreException(() -> pojoClass.getClazz().newInstance().toString());
    }
  }
}
