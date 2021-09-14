/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.entities;

import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SplunkCVConfigTest extends CategoryTest {
  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidateParams_whenServiceInstanceIdentifierIsUndefined() {
    assertThatThrownBy(() -> new SplunkCVConfig().validateParams())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("serviceInstanceIdentifier should not be null");
  }
}
