package io.harness.walktree.registries.visitorfield;

import io.harness.registries.RegistrableEntity;

public interface VisitableFieldProcessor<T extends VisitorFieldWrapper> extends RegistrableEntity {
  /**
   * This function will return string value for walktree impl for handling nodes having properties of custom java
   * classes like ParameterField.
   * @return String value
   */
  String getExpressionFieldValue(T actualField);

  /**
   * This function is used to update current field with required properties of given overrideField.
   * @param overrideField
   * @return T
   */
  T updateCurrentField(T actualField, T overrideField);

  /**
   * This function is used to create new field of custom java classes.
   */
  T createNewField(T actualField);

  /**
   * This function returns the error uuid corresponding to the field.
   */
  String getFieldWithStringValue(T actualField);

  /**
   * This functions creates new field with Error field as input.
   */
  T createNewFieldWithStringValue(String stringValue);
}
