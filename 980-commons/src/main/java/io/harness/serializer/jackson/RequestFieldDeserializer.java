/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.jackson;

import io.harness.utils.RequestField;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import java.io.IOException;

public class RequestFieldDeserializer extends StdDeserializer<RequestField<?>> implements ContextualDeserializer {
  private static final long serialVersionUID = 1L;

  protected final JavaType fullType;

  protected final JavaType referenceType;

  protected final transient JsonDeserializer<?> valueDeserializer;

  protected final transient TypeDeserializer valueTypeDeserializer;

  public RequestFieldDeserializer(
      JavaType fullType, JavaType refType, TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
    super(fullType);
    this.fullType = fullType;
    referenceType = refType;
    valueTypeDeserializer = typeDeser;
    valueDeserializer = valueDeser;
  }

  protected RequestFieldDeserializer withResolved(
      JavaType refType, TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
    if ((refType == referenceType) && (valueDeser == valueDeserializer) && (typeDeser == valueTypeDeserializer)) {
      return this;
    }
    return new RequestFieldDeserializer(fullType, refType, typeDeser, valueDeser);
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws JsonMappingException {
    JsonDeserializer<?> deser = valueDeserializer;
    TypeDeserializer typeDeser = valueTypeDeserializer;
    JavaType refType = referenceType;

    if (deser == null) {
      deser = ctxt.findContextualValueDeserializer(refType, property);
    } else { // otherwise directly assigned, probably not contextual yet:
      deser = ctxt.handleSecondaryContextualization(deser, property, refType);
    }
    if (typeDeser != null) {
      typeDeser = typeDeser.forProperty(property);
    }
    return withResolved(refType, typeDeser, deser);
  }

  @Override
  public RequestField<?> getNullValue(DeserializationContext ctxt) {
    return RequestField.ofNull();
  }

  @Override
  public RequestField<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    Object refd = (valueTypeDeserializer == null)
        ? valueDeserializer.deserialize(p, ctxt)
        : valueDeserializer.deserializeWithType(p, ctxt, valueTypeDeserializer);
    return RequestField.ofNullable(refd);
  }

  @Override
  public RequestField<?> deserializeWithType(
      JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
    final JsonToken t = p.getCurrentToken();
    if (t == JsonToken.VALUE_NULL) {
      return getNullValue(ctxt);
    }

    if (t != null && t.isScalarValue()) {
      return deserialize(p, ctxt);
    }
    return (RequestField<?>) typeDeserializer.deserializeTypedFromAny(p, ctxt);
  }
}
