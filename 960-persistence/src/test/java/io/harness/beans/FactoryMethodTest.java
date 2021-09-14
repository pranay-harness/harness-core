/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
