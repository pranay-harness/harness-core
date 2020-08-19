package io.harness.ng.core.models;

import org.codehaus.jackson.annotate.JsonProperty;

public enum InviteType {
  @JsonProperty("USER_INITIATED_INVITE") USER_INITIATED_INVITE("USER_INITIATED_INVITE"),
  @JsonProperty("ADMIN_INITIATED_INVITE") ADMIN_INITIATED_INVITE("ADMIN_INITIATED_INVITE");

  private String type;
  InviteType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
