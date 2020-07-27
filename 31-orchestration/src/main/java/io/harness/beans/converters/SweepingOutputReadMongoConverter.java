package io.harness.beans.converters;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.SweepingOutput;
import io.harness.serializer.KryoSerializer;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(CDC)
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
