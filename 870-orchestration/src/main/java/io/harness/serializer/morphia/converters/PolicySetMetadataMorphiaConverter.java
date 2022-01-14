package io.harness.serializer.morphia.converters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.governance.PolicySetMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicySetMetadataMorphiaConverter extends ProtoMessageConverter<PolicySetMetadata> {
  public PolicySetMetadataMorphiaConverter() {
    super(PolicySetMetadata.class);
  }
}
