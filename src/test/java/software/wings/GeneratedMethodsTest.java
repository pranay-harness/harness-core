package software.wings;

import com.openpojo.reflection.filters.FilterClassName;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public class GeneratedMethodsTest {
  private static final String POJO_PACKAGE = "software.wings";

  @Test
  public void testPojoStructureAndBehavior() {
    Validator validator = ValidatorBuilder.create().with(new SetterTester()).with(new GetterTester()).build();

    validator.validate(POJO_PACKAGE, new FilterClassName("^((?!Test$).)*$"));
  }

  public static boolean overridesMethod(Method method, Class<?> clazz) {
    return clazz == method.getDeclaringClass();
  }
}
