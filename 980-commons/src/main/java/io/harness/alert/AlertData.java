package io.harness.alert;

public interface AlertData {
  boolean matches(AlertData alertData);

  String buildTitle();

  default String buildResolutionTitle() {
    return null;
  }
}
