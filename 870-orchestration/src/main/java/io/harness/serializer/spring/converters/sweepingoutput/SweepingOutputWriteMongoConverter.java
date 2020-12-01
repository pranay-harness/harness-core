package io.harness.serializer.spring.converters.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class SweepingOutputWriteMongoConverter implements Converter<SweepingOutput, Binary> {
  private final KryoSerializer kryoSerializer;

  @Inject
  public SweepingOutputWriteMongoConverter(KryoSerializer kryoSerializer) {
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public Binary convert(SweepingOutput sweepingOutput) {
    return new Binary(kryoSerializer.asBytes(sweepingOutput));
  }
}
