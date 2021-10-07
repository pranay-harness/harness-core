package io.harness.serializer.spring.converters.governancemetadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.serializer.spring.ProtoReadConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class GovernanceMetadataReadConverter extends ProtoReadConverter<GovernanceMetadata> {
  public GovernanceMetadataReadConverter() {
    super(GovernanceMetadata.class);
  }
}
