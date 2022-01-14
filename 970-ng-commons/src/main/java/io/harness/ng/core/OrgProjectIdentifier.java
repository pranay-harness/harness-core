package io.harness.ng.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class OrgProjectIdentifier {
  private String orgIdentifier;
  private String projectIdentifier;
}
