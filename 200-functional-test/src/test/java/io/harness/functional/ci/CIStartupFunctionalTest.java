/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.ci;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.CIManagerExecutor;

import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIStartupFunctionalTest extends CategoryTest {
  @BeforeClass
  public static void setup() {
    RestAssured.useRelaxedHTTPSValidation();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(FunctionalTests.class)
  @Ignore("Not required Anymore as we have Rest Assured")
  public void shouldEnsureCIManagerStartsUp() {
    assertThatCode(() -> CIManagerExecutor.ensureCIManager(getClass())).doesNotThrowAnyException();
  }
}
