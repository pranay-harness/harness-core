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
    if (overrideField == null || actualField == null) {
      return actualField;
    }
    if (overrideField.isExpression()) {
      actualField.updateWithExpression(overrideField.getExpressionValue());
    } else {
      actualField.updateWithValue(overrideField.getValue());
    }
    return actualField;
  }

  @Override
  public ParameterField<?> cloneField(ParameterField<?> actualField) {
    if (actualField == null) {
      return null;
    }
    return new ParameterField<>(actualField.getValue(), actualField.isExpression(), actualField.getExpressionValue(),
        actualField.getInputSetValidator(), actualField.isTypeString());
  }

  @Override
  public String getFieldWithStringValue(ParameterField<?> actualField) {
    return actualField.isJsonResponseField() ? actualField.getResponseField() : "";
  }

  @Override
  public ParameterField<?> createNewFieldWithStringValue(String stringValue) {
    return ParameterField.createJsonResponseField(stringValue);
  }

  @Override
  public boolean isNull(ParameterField<?> actualField) {
    if (actualField.getExpressionValue() != null || actualField.getInputSetValidator() != null
        || actualField.getResponseField() != null || actualField.getValue() != null) {
      return false;
    }
    // Every flag should be false.
    return !actualField.isExpression() && !actualField.isJsonResponseField() && !actualField.isTypeString();
  }
}
