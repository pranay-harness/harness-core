package io.harness.beans.converters;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.serializer.KryoUtils;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(CDC)
public class SweepingOutputReadMongoConverter implements Converter<Binary, SweepingOutput> {
  @Override
  public SweepingOutput convert(Binary dbBytes) {
    return (SweepingOutput) KryoUtils.asObject(dbBytes.getData());
  }
}
