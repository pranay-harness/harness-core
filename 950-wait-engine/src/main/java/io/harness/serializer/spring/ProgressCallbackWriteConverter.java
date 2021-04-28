package io.harness.serializer.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class ProgressCallbackWriteConverter implements Converter<ProgressCallback, Binary> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Binary convert(ProgressCallback callback) {
    if (callback == null) {
      return null;
    }
    return new Binary(kryoSerializer.asDeflatedBytes(callback));
  }
}
