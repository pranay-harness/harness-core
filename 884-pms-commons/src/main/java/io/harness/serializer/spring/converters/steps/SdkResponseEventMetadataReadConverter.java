package io.harness.serializer.spring.converters.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.SdkResponseEventMetadata;
import io.harness.serializer.spring.ProtoReadConverter;

@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventMetadataReadConverter extends ProtoReadConverter<SdkResponseEventMetadata> {
  public SdkResponseEventMetadataReadConverter() {
    super(SdkResponseEventMetadata.class);
  }
}
