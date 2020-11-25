package io.harness.delegate.beans;

import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RemoteMethodReturnValueData implements DelegateResponseData {
  private Object returnValue;
  @Bind(JavaSerializer.class) private Throwable exception;
}
