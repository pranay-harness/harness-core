package io.harness.serializer.morphia.converters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.governance.GovernanceMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public class GovernanceMetadataMorphiaConverter extends ProtoMessageConverter<GovernanceMetadata> {
  public GovernanceMetadataMorphiaConverter() {
    super(GovernanceMetadata.class);
  }
}
