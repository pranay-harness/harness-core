package io.harness.serializer.spring.converters.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(PIPELINE)
@Singleton
@ReadingConverter
public class InterruptEffectReadConverter extends ProtoReadConverter<InterruptEffectProto> {
  public InterruptEffectReadConverter() {
    super(InterruptEffectProto.class);
  }
}
