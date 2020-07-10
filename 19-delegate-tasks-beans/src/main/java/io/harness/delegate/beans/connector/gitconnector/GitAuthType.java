package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GitAuthType {
  HTTP("Http"),
  SSH("Ssh");

  private final String displayName;

  GitAuthType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
