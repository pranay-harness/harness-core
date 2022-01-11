/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.validation;

import static io.harness.exception.WingsException.USER;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.YOGESH;
import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.nullCheckForInvalidRequest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ValidatorTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testStringTypeCheck() {
    assertThatThrownBy(() -> ensureType(String.class, 1, "Not of string type"));
    ensureType(String.class, "abc", "Not of string type");
    ensureType(Integer.class, 1, "Not of integer type");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testNullCheckForInvalidRequest() {
    nullCheckForInvalidRequest(new Object(), "message", USER);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> nullCheckForInvalidRequest(null, "message", USER));
  }
}
