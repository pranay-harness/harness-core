/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationError {
  private String fieldId;
  private String error;

  private ValidationError() {}

  public static ValidationError of(String field, String error) {
    ValidationError validationError = new ValidationError();
    validationError.setFieldId(field);
    validationError.setError(error);
    return validationError;
  }
}
