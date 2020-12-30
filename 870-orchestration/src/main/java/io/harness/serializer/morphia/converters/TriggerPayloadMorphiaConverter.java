package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.triggers.TriggerPayload;

public class TriggerPayloadMorphiaConverter extends ProtoMessageConverter<TriggerPayload> {
  public TriggerPayloadMorphiaConverter() {
    super(TriggerPayload.class);
  }
}
