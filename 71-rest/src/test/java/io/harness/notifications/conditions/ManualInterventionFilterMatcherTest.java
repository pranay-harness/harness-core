package io.harness.notifications.conditions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.alert.Alert.AlertBuilder.anAlert;

import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import io.harness.notifications.beans.ManualInterventionAlertFilters;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ManualInterventionNeededAlert;

import java.util.Arrays;

public class ManualInterventionFilterMatcherTest extends WingsBaseTest {
  @Test
  public void testMatch() {
    AlertFilter filter = new AlertFilter(AlertType.ApprovalNeeded, new Conditions(Operator.MATCHING, null, null));
    Alert manualInterventionAlert = manualInterventionAlert();
    ManualInterventionFilterMatcher matcher = new ManualInterventionFilterMatcher(filter, manualInterventionAlert);
    assertFalse("should not match because alert types are different", matcher.matchesCondition());

    ManualInterventionAlertFilters manualInterventionAlertFilters =
        new ManualInterventionAlertFilters(Arrays.asList("wrong-id"), Arrays.asList("some-env"));
    filter = new AlertFilter(
        AlertType.ManualInterventionNeeded, new Conditions(Operator.MATCHING, manualInterventionAlertFilters, null));
    matcher = new ManualInterventionFilterMatcher(filter, manualInterventionAlert);
    assertFalse("should not match because appIds don't match", matcher.matchesCondition());

    manualInterventionAlertFilters =
        new ManualInterventionAlertFilters(Arrays.asList("some-app"), Arrays.asList("some-env"));
    filter = new AlertFilter(
        AlertType.ManualInterventionNeeded, new Conditions(Operator.MATCHING, manualInterventionAlertFilters, null));
    matcher = new ManualInterventionFilterMatcher(filter, manualInterventionAlert);
    assertTrue(matcher.matchesCondition());
  }

  private Alert manualInterventionAlert() {
    ManualInterventionNeededAlert alertData = ManualInterventionNeededAlert.builder()
                                                  .envId("some-env")
                                                  .name("alert-name")
                                                  .executionId("some-exec-id")
                                                  .build();

    return anAlert()
        .withType(AlertType.ManualInterventionNeeded)
        .withAppId("some-app")
        .withAlertData(alertData)
        .build();
  }
}
