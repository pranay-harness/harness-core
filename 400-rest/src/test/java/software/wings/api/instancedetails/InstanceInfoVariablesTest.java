/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.instancedetails;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstanceInfoVariablesTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void defaultValueOfTrafficShift() {
    assertThat(InstanceInfoVariables.builder().build().getNewInstanceTrafficPercent()).isNull();
  }
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void isDeployStateInfo() {
    assertThat(InstanceInfoVariables.builder()
                   .instanceDetails(new ArrayList<>())
                   .instanceElements(new ArrayList<>())
                   .build()
                   .isDeployStateInfo())
        .isTrue();
    assertThat(InstanceInfoVariables.builder()
                   .instanceDetails(new ArrayList<>())
                   .newInstanceTrafficPercent(5)
                   .build()
                   .isDeployStateInfo())
        .isFalse();
  }
}
