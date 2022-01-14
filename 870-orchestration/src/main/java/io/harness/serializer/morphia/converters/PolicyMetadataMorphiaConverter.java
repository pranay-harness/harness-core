package io.harness.serializer.morphia.converters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.governance.PolicyMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicyMetadataMorphiaConverter extends ProtoMessageConverter<PolicyMetadata> {
  public PolicyMetadataMorphiaConverter() {
    super(PolicyMetadata.class);
  }
}
