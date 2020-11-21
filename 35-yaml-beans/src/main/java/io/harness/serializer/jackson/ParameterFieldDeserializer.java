package io.harness.serializer.jackson;

import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;

import io.harness.beans.InputSetValidator;
import io.harness.beans.InputSetValidatorType;
import io.harness.beans.ParameterField;
import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidArgumentsException;

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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ParameterFieldDeserializer extends StdDeserializer<ParameterField<?>> implements ContextualDeserializer {
  private static final long serialVersionUID = 1L;
  protected final JavaType fullType;
  protected final JavaType referenceType;
  protected transient JsonDeserializer<?> valueDeserializer;
  protected transient TypeDeserializer valueTypeDeserializer;

  private final List<InputSetValidatorTypeWithPattern> inputSetValidationPatternList;

  public ParameterFieldDeserializer(
      JavaType fullType, JavaType refType, TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
    super(fullType);
    this.fullType = fullType;
    referenceType = refType;
    valueTypeDeserializer = typeDeser;
    valueDeserializer = valueDeser;
    inputSetValidationPatternList = initialiseInputSetValidationPatternList();
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
  public ParameterField<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String text = p.getText().trim();
    boolean isTypeString = this.referenceType.getRawClass().equals(String.class);

    if (NGExpressionUtils.matchesInputSetPattern(text)) {
      return ParameterField.createExpressionField(
          true, NGExpressionUtils.DEFAULT_INPUT_SET_EXPRESSION, getInputSetValidator(text), isTypeString);
    }
    if (matchesVariablePattern(text)) {
      return ParameterField.createExpressionField(true, text, null, isTypeString);
    }

    Object refd = (valueTypeDeserializer == null)
        ? valueDeserializer.deserialize(p, ctxt)
        : valueDeserializer.deserializeWithType(p, ctxt, valueTypeDeserializer);

    return ParameterField.createValueField(refd);
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

  private InputSetValidator getInputSetValidator(String field) {
    for (InputSetValidatorTypeWithPattern validatorTypeWithPattern : inputSetValidationPatternList) {
      if (NGExpressionUtils.containsPattern(validatorTypeWithPattern.validatorPattern, field)) {
        // This will get the content inside the validation pattern.
        String validationParameters = validatorTypeWithPattern.validatorPattern.split(field)[1];
        return new InputSetValidator(validatorTypeWithPattern.validatorType,
            validationParameters.substring(0, validationParameters.length() - 1));
      }
    }
    // If the flow reaches here, then there is no validator, then value should match ${input}
    if (!field.equals(NGExpressionUtils.DEFAULT_INPUT_SET_EXPRESSION)) {
      throw new InvalidArgumentsException("Unsupported Input Set value");
    }
    return null;
  }

  private List<InputSetValidatorTypeWithPattern> initialiseInputSetValidationPatternList() {
    List<InputSetValidatorTypeWithPattern> validatorTypeWithPatterns = new LinkedList<>();
    for (InputSetValidatorType value : InputSetValidatorType.values()) {
      String pattern = NGExpressionUtils.getInputSetValidatorPattern(value.getYamlName());
      validatorTypeWithPatterns.add(new InputSetValidatorTypeWithPattern(value, Pattern.compile(pattern)));
    }
    return validatorTypeWithPatterns;
  }

  public static class InputSetValidatorTypeWithPattern {
    InputSetValidatorType validatorType;
    Pattern validatorPattern;

    public InputSetValidatorTypeWithPattern(InputSetValidatorType validatorType, Pattern validatorPattern) {
      this.validatorType = validatorType;
      this.validatorPattern = validatorPattern;
    }
  }
}
