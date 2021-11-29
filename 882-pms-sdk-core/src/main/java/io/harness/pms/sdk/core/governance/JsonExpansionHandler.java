package io.harness.pms.sdk.core.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface JsonExpansionHandler {
  ExpansionResponseBatch expandNode(ExpansionRequestBatch expansionRequest);
}
