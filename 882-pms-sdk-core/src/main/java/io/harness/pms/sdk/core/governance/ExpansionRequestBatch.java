package io.harness.pms.sdk.core.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class ExpansionRequestBatch {
  String service;
  Set<ExpansionRequest> expansionRequests;
}
