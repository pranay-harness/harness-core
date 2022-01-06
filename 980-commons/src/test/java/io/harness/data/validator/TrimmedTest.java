/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.validator;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TrimmedTest extends CategoryTest {
  @Builder
  static class TrimmedTestStructure {
    @Trimmed String str;
  }

  @Test
  @Owner(developers = GEORGE)
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
