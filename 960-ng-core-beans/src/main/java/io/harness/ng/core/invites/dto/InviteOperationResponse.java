/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum InviteOperationResponse {
  ACCOUNT_INVITE_ACCEPTED_NEED_PASSWORD("ACCOUNT_INVITE_ACCEPTED_NEED_PASSWORD"),
  ACCOUNT_INVITE_ACCEPTED("ACCOUNT_INVITE_ACCEPTED"),
  USER_INVITED_SUCCESSFULLY("USER_INVITED_SUCCESSFULLY"),
  USER_ALREADY_ADDED("USER_ALREADY_ADDED"),
  USER_ALREADY_INVITED("USER_ALREADY_INVITED"),
  FAIL("FAIL"),
  INVITE_EXPIRED("INVITE_EXPIRED"),
  INVITE_INVALID("INVITE_INVALID");

  private String type;
  InviteOperationResponse(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
