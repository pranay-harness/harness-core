package io.harness.serializer.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(PIPELINE)
@Singleton
@ReadingConverter
public class ProgressCallbackReadConverter implements Converter<Binary, ProgressCallback> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public ProgressCallback convert(Binary data) {
    if (data == null) {
      return null;
    }
    return (ProgressCallback) kryoSerializer.asInflatedObject(data.getData());
  }
}
