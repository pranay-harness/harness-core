package io.harness.serializer.json;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.PolicyMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicyMetadataSerializer extends ProtoJsonSerializer<PolicyMetadata> {
  public PolicyMetadataSerializer() {
    super(PolicyMetadata.class);
  }
}
