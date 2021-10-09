package io.harness.serializer.spring.converters.orchestrationMap;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.KryoSerializer;

import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(PIPELINE)
public abstract class OrchestrationMapAbstractWriteConverter<T extends OrchestrationMap>
    implements Converter<T, Binary> {
  private final KryoSerializer kryoSerializer;

  public OrchestrationMapAbstractWriteConverter(KryoSerializer kryoSerializer) {
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public Binary convert(T object) {
    if (object == null) {
      return null;
    }
    return new Binary(kryoSerializer.asDeflatedBytes(object));
  }
}
