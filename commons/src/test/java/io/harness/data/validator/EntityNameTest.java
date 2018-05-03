package io.harness.data.validator;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.Builder;
import org.junit.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class EntityNameTest {
  @Builder
  static class EntityNameTestStructure {
    @EntityName String str;
  }

  @Test
  public void testAllowedCharSet() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    String nonAllowed = "!@#$%^&*()+=\\/[]{}|~";
    for (char ch : nonAllowed.toCharArray()) {
      assertThat(validator.validate(EntityNameTestStructure.builder().str(String.format("foo%c", ch)).build()))
          .isNotEmpty();
    }
    String allowed = "_- ";
    for (char ch : allowed.toCharArray()) {
      assertThat(validator.validate(EntityNameTestStructure.builder().str(String.format("foo%c", ch)).build()))
          .isEmpty();
    }
  }
}
