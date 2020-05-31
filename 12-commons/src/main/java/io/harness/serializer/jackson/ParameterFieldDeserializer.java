package io.harness.serializer.jackson;

import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;

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
import io.harness.utils.ParameterField;

import java.io.IOException;

public class ParameterFieldDeserializer extends StdDeserializer<ParameterField<?>> implements ContextualDeserializer {
  private static final long serialVersionUID = 1L;

  protected final JavaType fullType;

  protected final JavaType referenceType;

  protected transient JsonDeserializer<?> valueDeserializer;

  protected transient TypeDeserializer valueTypeDeserializer;

  public ParameterFieldDeserializer(
      JavaType fullType, JavaType refType, TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
    super(fullType);
    this.fullType = fullType;
    referenceType = refType;
    valueTypeDeserializer = typeDeser;
    valueDeserializer = valueDeser;
  }

  protected ParameterFieldDeserializer withResolved(
      JavaType refType, TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
    if ((refType == referenceType) && (valueDeser == valueDeserializer) && (typeDeser == valueTypeDeserializer)) {
      return this;
    }
    return new ParameterFieldDeserializer(fullType, refType, typeDeser, valueDeser);
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
  public ParameterField<?> getNullValue(DeserializationContext ctxt) {
    return ParameterField.ofNull();
  }

  @Override
  public ParameterField deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String text = p.getText();

    if (matchesVariablePattern(text)) {
      return ParameterField.createField(null, true, text);
    }

    Object refd = (valueTypeDeserializer == null)
        ? valueDeserializer.deserialize(p, ctxt)
        : valueDeserializer.deserializeWithType(p, ctxt, valueTypeDeserializer);

    return ParameterField.createField(refd);
  }

  @Override
  public ParameterField<?> deserializeWithType(
      JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
    final JsonToken t = p.getCurrentToken();
    if (t == JsonToken.VALUE_NULL) {
      return getNullValue(ctxt);
    }

    if (t != null && t.isScalarValue()) {
      return deserialize(p, ctxt);
    }
    return (ParameterField<?>) typeDeserializer.deserializeTypedFromAny(p, ctxt);
  }
}
