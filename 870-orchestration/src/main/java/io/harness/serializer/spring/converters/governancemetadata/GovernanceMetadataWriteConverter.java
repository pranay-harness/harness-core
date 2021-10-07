package io.harness.serializer.spring.converters.governancemetadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.serializer.spring.ProtoWriteConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class GovernanceMetadataWriteConverter extends ProtoWriteConverter<GovernanceMetadata> {}
