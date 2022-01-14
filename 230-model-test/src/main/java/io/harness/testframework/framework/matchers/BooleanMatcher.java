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
