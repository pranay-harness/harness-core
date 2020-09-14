package io.harness.testframework.framework.matchers;

import software.wings.beans.artifact.Artifact;

import java.util.List;

public class ArtifactMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual == null) {
      return false;
    }
    List<Artifact> artifact = (List<Artifact>) actual;
    if (artifact.size() == 0) {
      return false;
    }
    return true;
  }
}
