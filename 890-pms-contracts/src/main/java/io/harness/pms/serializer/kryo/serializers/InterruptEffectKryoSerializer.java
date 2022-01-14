package io.harness.pms.serializer.kryo.serializers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(PIPELINE)
public class InterruptEffectKryoSerializer extends ProtobufKryoSerializer<InterruptEffectProto> {
  private static InterruptEffectKryoSerializer instance;

  public InterruptEffectKryoSerializer() {}

  public static synchronized InterruptEffectKryoSerializer getInstance() {
    if (instance == null) {
      instance = new InterruptEffectKryoSerializer();
    }
    return instance;
  }
}
