package io.harness.accesscontrol.commons.validation;

import io.harness.accesscontrol.common.validation.ValidationResult;

public interface HarnessActionValidator<T> {
  ValidationResult canDelete(T object);
  ValidationResult canCreate(T object);
  ValidationResult canUpdate(T object);
}
