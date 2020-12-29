package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.SimpleValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import java.util.Collections;
import java.util.Map;
import lombok.SneakyThrows;

public class ProtoRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
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

  @Override
  public boolean isSupported(final Class<?> c, final CastedField castedField) {
    return RecastReflectionUtils.implementsInterface(c, Message.class);
  }
}
