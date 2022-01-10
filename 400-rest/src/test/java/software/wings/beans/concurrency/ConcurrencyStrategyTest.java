/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.concurrency;

import static io.harness.rule.OwnerRule.PRASHANT;

import static software.wings.common.InfrastructureConstants.INFRA_ID_EXPRESSION;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.concurrency.ConcurrencyStrategy.UnitType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConcurrencyStrategyTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testIsEnabled() {
    ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.builder().build();
    assertThat(concurrencyStrategy.isEnabled()).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testNotEnabled() {
    ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.builder().unitType(UnitType.NONE).build();
    assertThat(concurrencyStrategy.isEnabled()).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testBuildFromUnit() {
    ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.buildFromUnit("INFRA");
    assertThat(concurrencyStrategy).isNotNull();
    assertThat(concurrencyStrategy.getUnitType()).isEqualTo(UnitType.INFRA);
    assertThat(concurrencyStrategy.getResourceUnit()).isEqualTo(INFRA_ID_EXPRESSION);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testInvalidUnit() {
    assertThatThrownBy(() -> ConcurrencyStrategy.buildFromUnit("RANDOM")).isInstanceOf(InvalidArgumentsException.class);
  }
}
