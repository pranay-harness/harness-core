package io.harness.ng.core.invites;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import org.codehaus.jackson.annotate.JsonProperty;

@OwnedBy(PL)
public enum InviteType {
  @JsonProperty("USER_INITIATED_INVITE") USER_INITIATED_INVITE("USER_INITIATED_INVITE"),
  @JsonProperty("ADMIN_INITIATED_INVITE") ADMIN_INITIATED_INVITE("ADMIN_INITIATED_INVITE");

  private final String type;
  InviteType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
