package io.serializer.jackson;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.ReferenceType;

public class NGHarnessSerializers extends Serializers.Base {
  @Override
  public JsonSerializer<?> findReferenceSerializer(SerializationConfig config, ReferenceType refType,
      BeanDescription beanDesc, TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentValueSerializer) {
    if (refType.hasRawClass(ParameterField.class)) {
      return new ParameterFieldSerializer();
    }
    return null;
  }
}
