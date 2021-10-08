package io.harness.serializer.spring.converters.governancemetadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.PolicyMetadata;
import io.harness.serializer.spring.ProtoReadConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicyMetadataReadConverter extends ProtoReadConverter<PolicyMetadata> {
  public PolicyMetadataReadConverter() {
    super(PolicyMetadata.class);
  }
}
