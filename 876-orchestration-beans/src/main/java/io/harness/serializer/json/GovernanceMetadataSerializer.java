package io.harness.serializer.json;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.GovernanceMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public class GovernanceMetadataSerializer extends ProtoJsonSerializer<GovernanceMetadata> {
  public GovernanceMetadataSerializer() {
    super(GovernanceMetadata.class);
  }
}
