package io.harness.data.validator;

import lombok.Builder;
import org.junit.Test;

public class EntityNameTest {
  @Builder
  static class EntityNameTestStructure {
    @EntityName String str;
  }

  @Test
  public void testAllowedCharSet() {
    /*
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
    }*/
  }
}
