package io.harness.serializer.recaster.proto;

import io.harness.beans.CastedField;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.bson.Document;

public class ProtoRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  @SneakyThrows
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    Object decodedObject = RecastOrchestrationUtils.getEncodedValue((Document) fromObject);

    if (decodedObject == null) {
      return null;
    }
    Message.Builder builder = (Message.Builder) targetClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(decodedObject.toString(), builder);
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
