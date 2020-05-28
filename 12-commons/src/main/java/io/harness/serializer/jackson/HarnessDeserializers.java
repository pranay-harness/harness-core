package io.harness.serializer.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import io.harness.utils.ParameterField;
import io.harness.utils.RequestField;

public class HarnessDeserializers extends Deserializers.Base {
  @Override
  public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType, DeserializationConfig config,
      BeanDescription beanDesc, TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer)
      throws JsonMappingException {
    if (refType.hasRawClass(RequestField.class)) {
      JavaType valueType = refType.getReferencedType();
      return new RequestFieldDeserializer(refType, valueType, contentTypeDeserializer, contentDeserializer);
    }
    if (refType.hasRawClass(ParameterField.class)) {
      JavaType valueType = refType.getReferencedType();
      return new ParameterFieldDeserializer(refType, valueType, contentTypeDeserializer, contentDeserializer);
    }
    return null;
  }
}
