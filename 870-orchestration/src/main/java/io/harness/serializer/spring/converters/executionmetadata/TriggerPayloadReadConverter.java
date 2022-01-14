package io.harness.serializer.spring.converters.executionmetadata;

import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.serializer.spring.ProtoReadConverter;

public class TriggerPayloadReadConverter extends ProtoReadConverter<TriggerPayload> {
  public TriggerPayloadReadConverter() {
    super(TriggerPayload.class);
  }
}
