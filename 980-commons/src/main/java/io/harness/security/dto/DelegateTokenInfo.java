package io.harness.security.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenInfo {
  private String name;
  private String ownerIdentifier;
}
