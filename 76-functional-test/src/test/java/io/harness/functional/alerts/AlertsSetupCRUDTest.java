package io.harness.functional.alerts;

import static graphql.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonObject;

import io.harness.RestUtils.AlertsRestUtil;
import io.harness.RestUtils.UserGroupRestUtil;
import io.harness.Utils.AlertsUtils;
import io.harness.Utils.TestUtils;
import io.harness.Utils.UserGroupUtils;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.alerts.AlertCategory;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.AlertType;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class AlertsSetupCRUDTest extends AbstractFunctionalTest {
  UserGroupRestUtil userGroupRestUtil = new UserGroupRestUtil();
  AlertsRestUtil arUtil = new AlertsRestUtil();

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void alertsCRUD() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(getUserGroup().getUuid());
    AlertNotificationRule createdAlert =
        createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    logger.info("Updating the alerts notification rule");
    AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRule(
        getAccount().getUuid(), userGroups, AlertCategory.Setup, AlertType.DelegateProfileError);
    AlertNotificationRule updatedAlert =
        arUtil.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

    logger.info("Verifying the updated alerts notification rule");
    assertNotNull(updatedAlert);
    assertTrue(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name()));
    assertFalse(updatedAlert.getAlertFilter().getAlertType().name().equals(
        createdAlert.getAlertFilter().getAlertType().name()));

    logger.info("Delete the alert");
    arUtil.deleteAlerts(getAccount().getUuid(), bearerToken, updatedAlert.getUuid());
    logger.info("Verify if the deleted alert does not exist");
    List<AlertNotificationRule> alertsList = arUtil.listAlerts(getAccount().getUuid(), bearerToken);
    assertFalse(AlertsUtils.isAlertAvailable(alertsList, createdAlert));
  }

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void updateAllAlertTypes() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(getUserGroup().getUuid());
    AlertNotificationRule createdAlert = createAndVerifyAlerts(
        userGroups, AlertCategory.ContinuousVerification, AlertType.CONTINUOUS_VERIFICATION_ALERT);

    List<AlertType> cvAlertTypes = AlertsUtils.getCVAlertTypes();
    cvAlertTypes.remove(AlertType.CONTINUOUS_VERIFICATION_ALERT);
    String previous = AlertType.CONTINUOUS_VERIFICATION_ALERT.name();

    logger.info("Running update test to see if all alert types are updateable");
    AlertNotificationRule updatedAlert = updateAndVerifyAllTypes(cvAlertTypes, previous, userGroups, createdAlert);
    deleteAlertNotificationRules(createdAlert, updatedAlert);

    createdAlert = createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    List<AlertType> setupAlertTypes = AlertsUtils.getSetupAlertTypes();
    setupAlertTypes.remove(AlertType.DelegatesDown);
    previous = AlertType.DelegatesDown.name();

    logger.info("Running update test to see if all alert types are updateable");
    updatedAlert = updateAndVerifyAllTypes(setupAlertTypes, previous, userGroups, createdAlert);
    deleteAlertNotificationRules(createdAlert, updatedAlert);
  }

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void updateAlertConditions() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(getUserGroup().getUuid());
    AlertNotificationRule createdAlert =
        createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    logger.info("Updating the alerts notification rule");
    AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRuleWithConditions(getAccount().getUuid(),
        userGroups, AlertCategory.Setup, AlertType.DelegatesDown, new Conditions(Operator.NOT_MATCHING, null, null));
    AlertNotificationRule updatedAlert =
        arUtil.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

    logger.info("Verifying the updated alerts notification rule");
    assertNotNull(updatedAlert);
    assertTrue(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name()));
    assertTrue(updatedAlert.getAlertFilter().getAlertType().name().equals(
        createdAlert.getAlertFilter().getAlertType().name()));
    assertTrue(updatedAlert.getAlertFilter().getConditions().getOperator().name().equals(Operator.NOT_MATCHING.name()));

    deleteAlertNotificationRules(createdAlert, updatedAlert);
  }

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void updateAlertCategory() {
    Set<String> userGroups = new HashSet<>();
    userGroups.add(getUserGroup().getUuid());
    AlertNotificationRule createdAlert =
        createAndVerifyAlerts(userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    logger.info("Updating the alerts notification rule");
    AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRule(getAccount().getUuid(), userGroups,
        AlertCategory.ContinuousVerification, AlertType.CONTINUOUS_VERIFICATION_ALERT);
    AlertNotificationRule updatedAlert =
        arUtil.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

    logger.info("Verifying the updated alerts notification rule");
    assertNotNull(updatedAlert);
    assertFalse(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name()));
    assertTrue(updatedAlert.getAlertCategory().name().equals(AlertCategory.ContinuousVerification.name()));
    assertFalse(updatedAlert.getAlertFilter().getAlertType().name().equals(
        createdAlert.getAlertFilter().getAlertType().name()));
    assertTrue(
        updatedAlert.getAlertFilter().getAlertType().name().equals(AlertType.CONTINUOUS_VERIFICATION_ALERT.name()));

    deleteAlertNotificationRules(createdAlert, updatedAlert);
  }

  private UserGroup getUserGroup() {
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertNotNull(userGroup);

    logger.info("Creating a Notification Settings with an email id and slack webhook");
    String emailId = TestUtils.generateRandomUUID() + "@harness.mailinator.com";
    String slackWebHook = new ScmSecret().decryptToString(new SecretName("slack_webhook_for_alert"));
    NotificationSettings notificationSettings = UserGroupUtils.createNotificationSettings(emailId, slackWebHook);
    userGroup.setNotificationSettings(notificationSettings);
    logger.info("Update user group with notification settings");
    userGroup = userGroupRestUtil.updateNotificationSettings(getAccount(), bearerToken, userGroup);
    assertNotNull(userGroup);
    assertNotNull(userGroup.getNotificationSettings());
    return userGroup;
  }

  private AlertNotificationRule createAndVerifyAlerts(
      Set<String> userGroups, AlertCategory alertCategory, AlertType alertType) {
    logger.info("Create a Setup Alert with Type : DelegatesDown");
    AlertNotificationRule alertNotificationRule =
        AlertsUtils.createAlertNotificationRule(getAccount().getUuid(), userGroups, alertCategory, alertType);

    AlertNotificationRule createdAlert = arUtil.createAlert(getAccount().getUuid(), bearerToken, alertNotificationRule);
    logger.info("Verify if the created alert exists");
    List<AlertNotificationRule> alertsList = arUtil.listAlerts(getAccount().getUuid(), bearerToken);
    assertTrue(alertsList.size() > 0);
    assertTrue(AlertsUtils.isAlertAvailable(alertsList, createdAlert));
    return createdAlert;
  }

  private void deleteAlertNotificationRules(AlertNotificationRule createdAlert, AlertNotificationRule updatedAlert) {
    logger.info("Delete the alert");
    arUtil.deleteAlerts(getAccount().getUuid(), bearerToken, updatedAlert.getUuid());
    logger.info("Verify if the deleted alert does not exist");
    List<AlertNotificationRule> alertsList = arUtil.listAlerts(getAccount().getUuid(), bearerToken);
    assertFalse(AlertsUtils.isAlertAvailable(alertsList, createdAlert));
  }

  private AlertNotificationRule updateAndVerifyAllTypes(
      List<AlertType> alertTypeList, String previous, Set<String> userGroups, AlertNotificationRule createdAlert) {
    AlertNotificationRule updatedAlert = null;
    for (AlertType alertType : alertTypeList) {
      logger.info("Updating the alert type from : " + previous + ": to : " + alertType.name());
      AlertNotificationRule updatedRule = AlertsUtils.createAlertNotificationRule(
          getAccount().getUuid(), userGroups, createdAlert.getAlertCategory(), alertType);

      updatedAlert = arUtil.updateAlert(getAccount().getUuid(), bearerToken, createdAlert.getUuid(), updatedRule);

      logger.info("Verifying the updated alerts notification rule");
      assertNotNull(updatedAlert);
      assertTrue(updatedAlert.getAlertCategory().name().equals(createdAlert.getAlertCategory().name()));
      assertFalse(updatedAlert.getAlertFilter().getAlertType().name().equals(
          createdAlert.getAlertFilter().getAlertType().name()));
      assertTrue(updatedAlert.getAlertFilter().getAlertType().name().equals(alertType.name()));
      previous = alertType.name();
    }
    return updatedAlert;
  }
}
