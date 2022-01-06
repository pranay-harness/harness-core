/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
