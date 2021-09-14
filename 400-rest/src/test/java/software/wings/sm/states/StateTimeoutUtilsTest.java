/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.sm.states.utils.StateTimeoutUtils;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StateTimeoutUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getTimeoutInMillis() {
    assertThat(StateTimeoutUtils.getTimeoutMillisFromMinutes(null)).isNull();
    assertThat(StateTimeoutUtils.getTimeoutMillisFromMinutes(0)).isNull();
    assertThat(StateTimeoutUtils.getTimeoutMillisFromMinutes(10)).isEqualTo(600000);
    assertThat(StateTimeoutUtils.getTimeoutMillisFromMinutes(Integer.MAX_VALUE)).isEqualTo(null);
  }
}
