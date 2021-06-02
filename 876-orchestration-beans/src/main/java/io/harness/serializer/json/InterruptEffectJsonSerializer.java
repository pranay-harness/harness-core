package io.harness.serializer.json;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;

@OwnedBy(PIPELINE)
public class InterruptEffectJsonSerializer extends ProtoJsonSerializer<InterruptEffectProto> {
  public InterruptEffectJsonSerializer() {
    super(InterruptEffectProto.class);
  }
}
