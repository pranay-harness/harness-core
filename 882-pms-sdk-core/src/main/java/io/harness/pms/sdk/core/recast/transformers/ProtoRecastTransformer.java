package io.harness.pms.sdk.core.recast.transformers;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import java.util.Collections;
import lombok.SneakyThrows;

public class ProtoRecastTransformer extends RecastTransformer {
  public ProtoRecastTransformer() {
    super(Collections.singletonList(Message.class));
  }

  @SneakyThrows
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }
    Builder builder = (Builder) targetClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(fromObject.toString(), builder);
    return builder.build();
  }

  @SneakyThrows
  @Override
  public Object encode(Object value, CastedField castedField) {
    if (value == null) {
      return null;
    }
    return JsonFormat.printer().print((Message) value);
  }
}
