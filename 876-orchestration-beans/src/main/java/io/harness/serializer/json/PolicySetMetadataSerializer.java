package io.harness.serializer.json;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.PolicySetMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicySetMetadataSerializer extends ProtoJsonSerializer<PolicySetMetadata> {
  public PolicySetMetadataSerializer() {
    super(PolicySetMetadata.class);
  }
}
