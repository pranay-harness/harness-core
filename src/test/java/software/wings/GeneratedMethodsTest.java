package software.wings;

import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.reflection.filters.FilterClassName;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.NoFieldShadowingRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.Test;
import software.wings.utils.ToStringTester;

import java.lang.reflect.Modifier;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public class GeneratedMethodsTest {
  private static final String[] packagesToScan = {"software.wings.waitnotify", "software.wings.sm"};

  private static final PojoClassFilter[] classFilters = {pojoClass
      -> !pojoClass.getClazz().isInterface(),
      pojoClass
      -> !Modifier.isAbstract(pojoClass.getClazz().getModifiers()),
      new FilterClassName("^((?!Test$).)*$"), new FilterClassName("^((?!Tester$).)*$"),
      new FilterClassName("^((?!Test\\$[0-9]+$).)*$"), pojoClass -> {System.out.println(pojoClass.getName());
  return true;
}
}
;

@Test
public void testPojoStructureAndBehavior() {
  Validator validator =
      ValidatorBuilder.create().with(new SetterTester()).with(new GetterTester()).with(new ToStringTester()).build();

  for (String packageName : packagesToScan) {
    validator.validateRecursively(packageName, classFilters);
  }
}
}
