package io.harness.serializer.spring.converters.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.SdkResponseEventMetadata;
import io.harness.serializer.spring.ProtoWriteConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventMetadataWriteConverter extends ProtoWriteConverter<SdkResponseEventMetadata> {}
