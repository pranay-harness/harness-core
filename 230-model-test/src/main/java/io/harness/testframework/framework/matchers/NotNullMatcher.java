/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.framework.matchers;

import org.apache.commons.lang3.StringUtils;

public class NotNullMatcher implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    Boolean expectedVal = (Boolean) expected;
    Boolean actualVal = StringUtils.isNotBlank((String) actual);
    return expectedVal == actualVal;
  }
}
