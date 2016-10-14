package software.wings.utils;

import com.openpojo.reflection.PojoClass;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public class ToStringTester extends BaseTester {
  /* (non-Javadoc)
   * @see com.openpojo.validation.test.Tester#run(com.openpojo.reflection.PojoClass)
   */
  @Override
  public void run(PojoClass pojoClass) {
    if (overridesMethod(pojoClass.getClazz(), "toString")) {
      Misc.ignoreException(() -> pojoClass.getClazz().newInstance().toString());
    }
  }
}
