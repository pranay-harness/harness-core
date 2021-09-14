/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.expression.RegexFunctor;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class RegexFunctorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testExtract() {
    final RegexFunctor regexFunctor = new RegexFunctor();
    assertThat(regexFunctor.extract("[0-9]*", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
    assertThat(regexFunctor.extract("[0-9]+", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testExtractWithNegativeCases() {
    final RegexFunctor regexFunctor = new RegexFunctor();
    assertThat(regexFunctor.extract("[0-9]*", null)).isEqualTo("");
    assertThat(regexFunctor.extract("[0-9]+", "build-webservices.noarch.rpm")).isEqualTo("");
  }
}
