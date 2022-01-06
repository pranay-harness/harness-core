/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.notifications.conditions;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import io.harness.notifications.beans.ManualInterventionAlertFilters;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ManualInterventionNeededAlert;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ManualInterventionFilterMatcherTest extends WingsBaseTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testMatch() {
    AlertFilter filter = new AlertFilter(AlertType.ApprovalNeeded, new Conditions(Operator.MATCHING, null, null));
    Alert manualInterventionAlert = manualInterventionAlert();
    ManualInterventionFilterMatcher matcher = new ManualInterventionFilterMatcher(filter, manualInterventionAlert);
    assertThat(matcher.matchesCondition()).isFalse();

    ManualInterventionAlertFilters manualInterventionAlertFilters =
        new ManualInterventionAlertFilters(Arrays.asList("wrong-id"), Arrays.asList("some-env"));
    filter = new AlertFilter(
        AlertType.ManualInterventionNeeded, new Conditions(Operator.MATCHING, manualInterventionAlertFilters, null));
    matcher = new ManualInterventionFilterMatcher(filter, manualInterventionAlert);
    assertThat(matcher.matchesCondition()).isFalse();

    manualInterventionAlertFilters =
        new ManualInterventionAlertFilters(Arrays.asList("some-app"), Arrays.asList("some-env"));
    filter = new AlertFilter(
        AlertType.ManualInterventionNeeded, new Conditions(Operator.MATCHING, manualInterventionAlertFilters, null));
    matcher = new ManualInterventionFilterMatcher(filter, manualInterventionAlert);
    assertThat(matcher.matchesCondition()).isTrue();
  }

  private Alert manualInterventionAlert() {
    ManualInterventionNeededAlert alertData = ManualInterventionNeededAlert.builder()
                                                  .envId("some-env")
                                                  .name("alert-name")
                                                  .executionId("some-exec-id")
                                                  .build();

    return Alert.builder().type(AlertType.ManualInterventionNeeded).appId("some-app").alertData(alertData).build();
  }
}
