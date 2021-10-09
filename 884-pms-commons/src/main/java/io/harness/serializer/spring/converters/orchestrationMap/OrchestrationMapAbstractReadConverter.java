package io.harness.serializer.spring.converters.orchestrationMap;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.KryoSerializer;

import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(PIPELINE)
@SuppressWarnings("unchecked")
public abstract class OrchestrationMapAbstractReadConverter<T extends OrchestrationMap>
    implements Converter<Binary, T> {
  private final KryoSerializer kryoSerializer;

  public OrchestrationMapAbstractReadConverter(KryoSerializer kryoSerializer) {
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public T convert(Binary binary) {
    if (binary.getData() == null) {
      return null;
    }

    return (T) kryoSerializer.asInflatedObject(binary.getData());
  }
}
