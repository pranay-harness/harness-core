/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.framework.matchers;

public class BooleanMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual == null) {
      return false;
    }
    Boolean expectedVal = (Boolean) expected;
    Boolean actualVal = (Boolean) actual;
    return expectedVal == actualVal;
  }
}
