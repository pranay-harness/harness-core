package io.harness.serializer.recaster.proto;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.google.protobuf.ProtocolMessageEnum;
import lombok.SneakyThrows;

public class ProtoEnumRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  @SneakyThrows
  @Override
  @SuppressWarnings("unchecked")
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    String enumName = (String) fromObject;

    Class<? extends ProtocolMessageEnum> enumClass = (Class<? extends ProtocolMessageEnum>) targetClass;
    return enumClass.getMethod("valueOf", String.class).invoke(null, enumName);
  }

  @SneakyThrows
  @Override
  public Object encode(Object value, CastedField castedField) {
    if (value == null) {
      return null;
    }

    ProtocolMessageEnum protocolMessageEnum = (ProtocolMessageEnum) value;
    return protocolMessageEnum.getValueDescriptor().getName();
  }

  @Override
  public boolean isSupported(final Class<?> c, final CastedField castedField) {
    return RecastReflectionUtils.implementsInterface(c, ProtocolMessageEnum.class);
  }
}
