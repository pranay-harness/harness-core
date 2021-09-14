/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

@RunListener.ThreadSafe
@Slf4j
public class ExtendsCategoryEnforcer extends RunListener {
  @Override
  public void testStarted(Description description) throws Exception {
    Class<?> testClass = description.getTestClass();
    if (!(CategoryTest.class.isAssignableFrom(testClass)
            // hack needed for working across ClassLoaders (eg: Powermock)
            || superClassNames(testClass).contains(CategoryTest.class.getName()))) {
      log.error("Test {} does not extend CategoryTest", testClass);
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
