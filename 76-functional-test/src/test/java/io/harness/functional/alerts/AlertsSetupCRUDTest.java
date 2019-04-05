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
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.alerts.AlertCategory;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.AlertType;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlertsSetupCRUDTest extends AbstractFunctionalTest {
  private static final Logger logger = LoggerFactory.getLogger(AlertsSetupCRUDTest.class);
  UserGroupRestUtil userGroupRestUtil = new UserGroupRestUtil();
  AlertsRestUtil arUtil = new AlertsRestUtil();

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void alertsCRUD() {
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

    Set<String> userGroups = new HashSet<>();
    userGroups.add(userGroup.getUuid());

    logger.info("Create a Setup Alert with Type : DelegatesDown");
    AlertNotificationRule alertNotificationRule = AlertsUtils.createAlertNotificationRule(
        getAccount().getUuid(), userGroups, AlertCategory.Setup, AlertType.DelegatesDown);

    AlertNotificationRule createdAlert = arUtil.createAlert(getAccount().getUuid(), bearerToken, alertNotificationRule);
    logger.info("Verify if the created alert exists");
    List<AlertNotificationRule> alertsList = arUtil.listAlerts(getAccount().getUuid(), bearerToken);
    assertTrue(alertsList.size() > 0);
    assertTrue(AlertsUtils.isAlertAvailable(alertsList, createdAlert));

    logger.info("Delete the alert");
    arUtil.deleteAlerts(getAccount().getUuid(), bearerToken, createdAlert.getUuid());
    logger.info("Verify if the deleted alert does not exist");
    alertsList = arUtil.listAlerts(getAccount().getUuid(), bearerToken);
    assertFalse(AlertsUtils.isAlertAvailable(alertsList, createdAlert));
  }
}
