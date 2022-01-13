/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CategoryTimeoutRuleTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category({UnitTests.class, CategoryTimeoutRule.RunMode.class})
  public void testTheTimeoutCapability() {}
}
