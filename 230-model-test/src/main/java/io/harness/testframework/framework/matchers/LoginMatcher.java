package io.harness.testframework.framework.matchers;

import software.wings.beans.User;

public class LoginMatcher implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual == null) {
      return false;
    }
    User user = (User) actual;
    if (user.getToken() == null) {
      return false;
    }
    return true;
  }
}
