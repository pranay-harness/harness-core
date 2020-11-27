package io.harness.serializer.spring.converters.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.SweepingOutput;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class SweepingOutputReadMongoConverter implements Converter<Binary, SweepingOutput> {
  private final KryoSerializer kryoSerializer;

  @Inject
  public SweepingOutputReadMongoConverter(KryoSerializer kryoSerializer) {
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public SweepingOutput convert(Binary dbBytes) {
    return (SweepingOutput) kryoSerializer.asObject(dbBytes.getData());
  }
}
