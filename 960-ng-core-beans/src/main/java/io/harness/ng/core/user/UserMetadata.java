package io.harness.ng.core.user;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class UserMetadata {
  String name;
  String id;
  String emailId;
}
