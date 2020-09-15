package io.harness.serializer.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import io.harness.utils.RequestField;

public class HarnessDeserializers extends Deserializers.Base {
  @Override
  public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType, DeserializationConfig config,
      BeanDescription beanDesc, TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
    if (refType.hasRawClass(RequestField.class)) {
      JavaType valueType = refType.getReferencedType();
      return new RequestFieldDeserializer(refType, valueType, contentTypeDeserializer, contentDeserializer);
    }
    return null;
  }
}
