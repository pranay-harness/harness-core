/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.registries.visitorfield;

public interface VisitableFieldProcessor<T extends VisitorFieldWrapper> {
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
   * This function is used to create clone of field of custom java classes.
   */
  T cloneField(T actualField);

  /**
   * This function returns the error uuid corresponding to the field.
   */
  String getFieldWithStringValue(T actualField);

  /**
   * This functions creates new field with Error field as input.
   */
  T createNewFieldWithStringValue(String stringValue);

  /**
   * This function checks whether field is equivalent to null or not.
   * @param actualField
   * @return
   */
  boolean isNull(T actualField);
}
