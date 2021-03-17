package io.harness.pms.yaml;

import io.harness.exception.FunctorException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.NotExpression;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.validation.InputSetValidator;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.bson.Document;

@Getter
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "ParameterDocumentFieldKeys")
public class ParameterDocumentField {
  @NotExpression private String expressionValue;
  private boolean expression;
  @Setter private Document valueDoc;
  private String valueClass;
  private boolean typeString;

  // This field is set when runtime input with validation is given.
  private InputSetValidator inputSetValidator;

  // Below 2 fields are when caller wants to set String field instead of T like for some errors, input set merge, etc.
  private boolean jsonResponseField;
  private String responseField;

  @Builder
  public ParameterDocumentField(String expressionValue, boolean expression, Document valueDoc, String valueClass,
      boolean typeString, InputSetValidator inputSetValidator, boolean jsonResponseField, String responseField) {
    this.expressionValue = expressionValue;
    this.expression = expression;
    this.valueDoc = valueDoc;
    this.valueClass = valueClass;
    this.typeString = typeString;
    this.inputSetValidator = inputSetValidator;
    this.jsonResponseField = jsonResponseField;
    this.responseField = responseField;
  }

  public Object get(String key) {
    if (expression || valueDoc == null) {
      throw new FunctorException(String.format("Cannot access field '%s' of null object", key));
    }
    return ExpressionEvaluatorUtils.fetchField(valueDoc.get(ParameterFieldValueWrapper.VALUE_FIELD), key).orElse(null);
  }

  public void updateWithExpression(String newExpression) {
    expression = true;
    expressionValue = newExpression;
    valueDoc = null;
  }

  public void updateWithValue(Object newValue) {
    expression = false;
    expressionValue = null;
    valueDoc = RecastOrchestrationUtils.toDocument(new ParameterFieldValueWrapper<>(newValue));
  }

  public Object fetchFinalValue() {
    return expression ? expressionValue
                      : (valueDoc == null ? null : valueDoc.getOrDefault(ParameterFieldValueWrapper.VALUE_FIELD, null));
  }
}
