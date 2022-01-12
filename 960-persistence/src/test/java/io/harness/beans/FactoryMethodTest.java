/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.beans.TriggeredBy.triggeredBy;
import static io.harness.rule.OwnerRule.VGLIJIN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FactoryMethodTest extends CategoryTest {
  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void triggeredByTest() {
    String name = "name";
    String email = "name@harness.io";
    assertThat(triggeredBy(name, email)).isEqualTo(new TriggeredBy(name, email));
  }
}
