package io.harness.serializer.spring.converters.governancemetadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.PolicySetMetadata;
import io.harness.serializer.spring.ProtoReadConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicySetMetadataReadConverter extends ProtoReadConverter<PolicySetMetadata> {
  public PolicySetMetadataReadConverter() {
    super(PolicySetMetadata.class);
  }
}
