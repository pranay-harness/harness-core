package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._955_DELEGATE_BEANS)
public class RemoteMethodReturnValueData implements DelegateResponseData {
  private Object returnValue;
  @Bind(JavaSerializer.class) private Throwable exception;
}
