package io.harness.testframework.framework.matchers;

public class SettingsAttributeMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual == null) {
      return false;
    }
    return true;
  }
}
