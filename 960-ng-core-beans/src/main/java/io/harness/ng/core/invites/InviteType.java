/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.invites;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import org.codehaus.jackson.annotate.JsonProperty;

@OwnedBy(PL)
public enum InviteType {
  @JsonProperty("USER_INITIATED_INVITE") USER_INITIATED_INVITE("USER_INITIATED_INVITE"),
  @JsonProperty("ADMIN_INITIATED_INVITE") ADMIN_INITIATED_INVITE("ADMIN_INITIATED_INVITE"),
  @JsonProperty("SCIM_INITIATED_INVITE") SCIM_INITIATED_INVITE("SCIM_INITIATED_INVITE");

  private final String type;
  InviteType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
