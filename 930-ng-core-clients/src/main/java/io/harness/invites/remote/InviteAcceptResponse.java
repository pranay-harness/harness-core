package io.harness.invites.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.InviteOperationResponse;
import io.harness.ng.core.user.UserInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class InviteAcceptResponse {
  InviteOperationResponse response;
  UserInfo userInfo;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String inviteId;
  String email;
}
