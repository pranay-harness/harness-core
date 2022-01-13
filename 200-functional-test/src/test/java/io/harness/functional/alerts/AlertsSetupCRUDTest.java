/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.alerts;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.AlertsUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.restutils.AlertsRestUtils;

import software.wings.alerts.AlertCategory;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.AlertType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AlertsSetupCRUDTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void updateAllAlertTypes() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(
        UserGroupUtils.createUserGroupAndUpdateWithNotificationSettings(getAccount(), bearerToken).getUuid());
    AlertNotificationRule createdAlert = createAndVerifyAlerts(
        userGroups, AlertCategory.ContinuousVerification, AlertType.CONTINUOUS_VERIFICATION_ALERT);

    List<AlertType> cvAlertTypes = AlertsUtils.getCVAlertTypes();
    cvAlertTypes.remove(AlertType.CONTINUOUS_VERIFICATION_ALERT);
    String previous = AlertType.CONTINUOUS_VERIFICATION_ALERT.name();

    log.info("Running update test to see if all alert types are updateable");
    AlertNotificationRule updatedAlert = updateAndVerifyAllTypes(cvAlertTypes, previous, userGroups, createdAlert);
    deleteAlertNotificationRules(createdAlert, updatedAlert);

    createdAlert = createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    List<AlertType> setupAlertTypes = AlertsUtils.getSetupAlertTypes();
    setupAlertTypes.remove(AlertType.DelegatesDown);
    previous = AlertType.DelegatesDown.name();

    log.info("Running update test to see if all alert types are updateable");
    updatedAlert = updateAndVerifyAllTypes(setupAlertTypes, previous, userGroups, createdAlert);
    deleteAlertNotificationRules(createdAlert, updatedAlert);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void updateAlertConditions() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(
        UserGroupUtils.createUserGroupAndUpdateWithNotificationSettings(getAccount(), bearerToken).getUuid());
    AlertNotificationRule createdAlert =
        createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    log.info("Updating the alerts notification rule");
    AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRuleWithConditions(getAccount().getUuid(),
        userGroups, AlertCategory.Setup, AlertType.DelegatesDown, new Conditions(Operator.NOT_MATCHING, null, null));
    AlertNotificationRule updatedAlert =
        AlertsRestUtils.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

    log.info("Verifying the updated alerts notification rule");
    assertThat(updatedAlert).isNotNull();
    assertThat(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name())).isTrue();
    assertThat(
        updatedAlert.getAlertFilter().getAlertType().name().equals(createdAlert.getAlertFilter().getAlertType().name()))
        .isTrue();
    assertThat(updatedAlert.getAlertFilter().getConditions().getOperator().name().equals(Operator.NOT_MATCHING.name()))
        .isTrue();

    deleteAlertNotificationRules(createdAlert, updatedAlert);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void updateAlertCategory() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(
        UserGroupUtils.createUserGroupAndUpdateWithNotificationSettings(getAccount(), bearerToken).getUuid());
    AlertNotificationRule createdAlert =
        createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    log.info("Updating the alerts notification rule");
    AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRule(getAccount().getUuid(), userGroups,
        AlertCategory.ContinuousVerification, AlertType.CONTINUOUS_VERIFICATION_ALERT);
    AlertNotificationRule updatedAlert =
        AlertsRestUtils.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

    log.info("Verifying the updated alerts notification rule");
    assertThat(updatedAlert).isNotNull();
    assertThat(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name())).isFalse();
    assertThat(updatedAlert.getAlertCategory().name().equals(AlertCategory.ContinuousVerification.name())).isTrue();
    assertThat(
        updatedAlert.getAlertFilter().getAlertType().name().equals(createdAlert.getAlertFilter().getAlertType().name()))
        .isFalse();
    assertThat(
        updatedAlert.getAlertFilter().getAlertType().name().equals(AlertType.CONTINUOUS_VERIFICATION_ALERT.name()))
        .isTrue();

    deleteAlertNotificationRules(createdAlert, updatedAlert);
  }

  private AlertNotificationRule createAndVerifyAlerts(
      Set<String> userGroups, AlertCategory alertCategory, AlertType alertType) {
    log.info("Create a Setup Alert with Type : DelegatesDown");
    AlertNotificationRule alertNotificationRule =
        AlertsUtils.createAlertNotificationRule(getAccount().getUuid(), userGroups, alertCategory, alertType);

    AlertNotificationRule createdAlert =
        AlertsRestUtils.createAlert(getAccount().getUuid(), bearerToken, alertNotificationRule);
    log.info("Verify if the created alert exists");
    List<AlertNotificationRule> alertsList = AlertsRestUtils.listAlerts(getAccount().getUuid(), bearerToken);
    assertThat(alertsList.size() > 0).isTrue();
    assertThat(AlertsUtils.isAlertAvailable(alertsList, createdAlert)).isTrue();
    return createdAlert;
  }

  private void deleteAlertNotificationRules(AlertNotificationRule createdAlert, AlertNotificationRule updatedAlert) {
    log.info("Delete the alert");
    AlertsRestUtils.deleteAlerts(getAccount().getUuid(), bearerToken, updatedAlert.getUuid());
    log.info("Verify if the deleted alert does not exist");
    List<AlertNotificationRule> alertsList = AlertsRestUtils.listAlerts(getAccount().getUuid(), bearerToken);
    assertThat(AlertsUtils.isAlertAvailable(alertsList, createdAlert)).isFalse();
  }

  private AlertNotificationRule updateAndVerifyAllTypes(
      List<AlertType> alertTypeList, String previous, Set<String> userGroups, AlertNotificationRule createdAlert) {
    AlertNotificationRule updatedAlert = null;
    for (AlertType alertType : alertTypeList) {
      log.info("Updating the alert type from : " + previous + ": to : " + alertType.name());
      AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRule(
          getAccount().getUuid(), userGroups, createdAlert.getAlertCategory(), alertType);

      updatedAlert =
          AlertsRestUtils.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

      log.info("Verifying the updated alerts notification rule");
      assertThat(updatedAlert).isNotNull();
      assertThat(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name())).isTrue();
      assertThat(updatedAlert.getAlertFilter().getAlertType().name().equals(
                     createdAlert.getAlertFilter().getAlertType().name()))
          .isFalse();
      assertThat(updatedAlert.getAlertFilter().getAlertType().name().equals(alertType.name())).isTrue();
      previous = alertType.name();
    }
    return updatedAlert;
  }
}
