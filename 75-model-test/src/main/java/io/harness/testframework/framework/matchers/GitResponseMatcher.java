package io.harness.testframework.framework.matchers;

import io.harness.testframework.framework.git.GitData;

import java.util.List;

public class GitResponseMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual == null) {
      return false;
    }
    List<GitData> gitDataList = (List<GitData>) actual;
    if (gitDataList.size() == 0) {
      return false;
    }
    return true;
  }
}
