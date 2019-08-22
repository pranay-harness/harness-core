package software.wings.delegatetasks;

import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RemoteMethodReturnValueData implements ResponseData {
  private Object returnValue;
  @Bind(JavaSerializer.class) private Throwable exception;
}
