package io.harness.accesscontrol.scopes;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ScopeDTO {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
}
