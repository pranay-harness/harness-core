package io.harness.data.validator;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.utils.UuidUtils.base64StrToUuid;
import static io.harness.utils.UuidUtils.isValidUuidStr;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UuidValidator implements ConstraintValidator<Uuid, String> {
  @Override
  public void initialize(Uuid constraintAnnotation) {}

  private boolean isValidBase64EncodedUuid(String value) {
    boolean isValidBase64EncodedUuid = false;
    try {
      String uuidStr = base64StrToUuid(value);
      isValidBase64EncodedUuid = isValidUuidStr(uuidStr);
    } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
      log.info("{} is not a valid Base64 encoded UUID", value);
    }
    return isValidBase64EncodedUuid;
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (hasNone(value)) {
      return false;
    }
    return isValidUuidStr(value) || isValidBase64EncodedUuid(value);
  }
}
