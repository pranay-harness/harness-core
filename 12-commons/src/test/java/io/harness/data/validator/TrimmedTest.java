package io.harness.data.validator;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class TrimmedTest {
  @Builder
  static class TrimmedTestStructure {
    @Trimmed String str;
  }

  @Test
  @Category(UnitTests.class)
  public void testTrimmed() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    final Validator validator = factory.getValidator();

    assertThat(validator.validate(TrimmedTestStructure.builder().str(" a ").build())).isNotEmpty();
    assertThat(validator.validate(TrimmedTestStructure.builder().str("a ").build())).isNotEmpty();
    assertThat(validator.validate(TrimmedTestStructure.builder().str(" a").build())).isNotEmpty();

    assertThat(validator.validate(TrimmedTestStructure.builder().str(null).build())).isEmpty();
    assertThat(validator.validate(TrimmedTestStructure.builder().str("").build())).isEmpty();
    assertThat(validator.validate(TrimmedTestStructure.builder().str("abc").build())).isEmpty();
  }
}
