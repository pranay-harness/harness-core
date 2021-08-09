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
