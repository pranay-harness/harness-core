package io.harness;

import static org.assertj.core.api.Assertions.fail;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;
import java.util.List;

@RunListener.ThreadSafe
@Slf4j
public class ExtendsCategoryEnforcer extends RunListener {
  @Override
  public void testStarted(Description description) throws Exception {
    Class<?> testClass = description.getTestClass();
    if (!(CategoryTest.class.isAssignableFrom(testClass)
            // hack needed for working across ClassLoaders (eg: Powermock)
            || superClassNames(testClass).contains(CategoryTest.class.getName()))) {
      logger.error("Test {} does not extend CategoryTest", testClass);
      fail("Test classes should extend CategoryTest");
    }
  }

  private List<String> superClassNames(Class<?> testClass) {
    List<String> superclasses = new ArrayList<>();
    while (testClass != null) {
      superclasses.add(testClass.getName());
      testClass = testClass.getSuperclass();
    }
    return superclasses;
  }
}
