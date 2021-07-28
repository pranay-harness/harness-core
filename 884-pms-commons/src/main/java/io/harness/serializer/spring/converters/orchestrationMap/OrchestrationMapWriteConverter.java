package io.harness.serializer.spring.converters.orchestrationMap;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class OrchestrationMapWriteConverter implements Converter<OrchestrationMap, Binary> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Binary convert(OrchestrationMap orchestrationMap) {
    if (orchestrationMap == null) {
      return null;
    }
    return new Binary(kryoSerializer.asDeflatedBytes(orchestrationMap));
  }
}
