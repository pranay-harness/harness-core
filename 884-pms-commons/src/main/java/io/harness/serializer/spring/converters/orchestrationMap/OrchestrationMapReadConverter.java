package io.harness.serializer.spring.converters.orchestrationMap;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(PIPELINE)
@Singleton
@ReadingConverter
public class OrchestrationMapReadConverter implements Converter<Binary, OrchestrationMap> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public OrchestrationMap convert(Binary binary) {
    if (binary.getData() == null) {
      return null;
    }

    return (OrchestrationMap) kryoSerializer.asInflatedObject(binary.getData());
  }
}
