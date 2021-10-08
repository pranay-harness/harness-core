package io.harness.serializer.spring.converters.governancemetadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.PolicySetMetadata;
import io.harness.serializer.spring.ProtoWriteConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class PolicySetMetadataWriteConverter extends ProtoWriteConverter<PolicySetMetadata> {}
