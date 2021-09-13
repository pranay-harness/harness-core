package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.invites.dto.InviteOperationResponse;

@OwnedBy(PL)
public enum AddUserResponse {
  USER_INVITED_SUCCESSFULLY("USER_INVITED_SUCCESSFULLY"),
  USER_ADDED_SUCCESSFULLY("USER_ADDED_SUCCESSFULLY"),
  USER_ALREADY_ADDED("USER_ALREADY_ADDED"),
  USER_ALREADY_INVITED("USER_ALREADY_INVITED"),
  FAIL("FAIL");

  private final String type;
  AddUserResponse(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public static AddUserResponse fromInviteOperationResponse(InviteOperationResponse inviteOperationResponse) {
    switch (inviteOperationResponse) {
      case USER_INVITED_SUCCESSFULLY:
        return USER_INVITED_SUCCESSFULLY;
      case USER_ALREADY_ADDED:
        return USER_ALREADY_ADDED;
      case USER_ALREADY_INVITED:
        return USER_ALREADY_INVITED;
      case FAIL:
        return FAIL;
      default:
        throw new InvalidArgumentsException(
            String.format("Unknown InviteOperationResponse type %s", inviteOperationResponse));
    }
  }
}
