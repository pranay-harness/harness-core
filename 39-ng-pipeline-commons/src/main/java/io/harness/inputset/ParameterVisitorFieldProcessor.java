package io.harness.inputset;

import io.harness.beans.ParameterField;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;

public class ParameterVisitorFieldProcessor implements VisitableFieldProcessor<ParameterField<?>> {
  @Override
  public String getExpressionFieldValue(ParameterField<?> actualField) {
    return actualField.isExpression() ? actualField.getExpressionValue() : "";
  }

  @Override
  public ParameterField<?> updateCurrentField(ParameterField<?> actualField, ParameterField<?> overrideField) {
    actualField.updateWithValue(overrideField.getValue());
    return actualField;
  }

  @Override
  public ParameterField<?> createNewField(ParameterField<?> actualField) {
    return ParameterField.createExpressionField(
        true, actualField.getExpressionValue(), actualField.getInputSetValidator(), actualField.isTypeString());
  }

  @Override
  public String getFieldWithStringValue(ParameterField<?> actualField) {
    return actualField.isJsonResponseField() ? actualField.getResponseField() : "";
  }

  @Override
  public ParameterField<?> createNewFieldWithStringValue(String stringValue) {
    return ParameterField.createJsonResponseField(stringValue);
  }
}
