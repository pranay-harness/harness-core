package io.harness.serializer.morphia.converters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;

@OwnedBy(PIPELINE)
public class InterruptEffectMorphiaConverter extends ProtoMessageConverter<InterruptEffectProto> {
  public InterruptEffectMorphiaConverter() {
    super(InterruptEffectProto.class);
  }
}
