package io.harness.beans.converters;

import io.harness.beans.SweepingOutput;
import io.harness.serializer.KryoUtils;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

public class SweepingOutputWriteMongoConverter implements Converter<SweepingOutput, Binary> {
  @Override
  public Binary convert(SweepingOutput sweepingOutput) {
    return new Binary(KryoUtils.asBytes(sweepingOutput));
  }
}
