package io.harness.serializer.spring.converters.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.serializer.spring.ProtoWriteConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class InterruptEffectWriteConverter extends ProtoWriteConverter<InterruptEffectProto> {}
