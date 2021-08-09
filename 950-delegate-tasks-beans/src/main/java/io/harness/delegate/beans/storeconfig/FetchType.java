package io.harness.delegate.beans.storeconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FetchType {
  @JsonProperty("Branch") BRANCH("Branch"),
  @JsonProperty("Commit") COMMIT("Commit");

  private final String name;

  FetchType(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
