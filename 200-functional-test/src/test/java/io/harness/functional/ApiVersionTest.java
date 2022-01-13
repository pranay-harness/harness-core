/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional;

import static io.harness.rule.OwnerRule.SRINIVAS;

import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiVersionTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(FunctionalTests.class)
  public void shouldApiReady() {
    Setup.portal().when().get("/version").then().statusCode(200);
  }
}
