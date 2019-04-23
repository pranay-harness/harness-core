package io.harness.restutils;

import static junit.framework.TestCase.assertTrue;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.alert.AlertNotificationRule;

import java.util.List;
import javax.ws.rs.core.GenericType;

public class AlertsRestUtils {
  public static AlertNotificationRule createAlert(
      String accountId, String bearerToken, AlertNotificationRule alertNotificationRule) {
    RestResponse<AlertNotificationRule> alertRule =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .body(alertNotificationRule, ObjectMapperType.GSON)
            .post("/alert-notification-rules")
            .as(new GenericType<RestResponse<AlertNotificationRule>>() {}.getType());
    return alertRule.getResource();
  }

  public static List<AlertNotificationRule> listAlerts(String accountId, String bearerToken) {
    RestResponse<List<AlertNotificationRule>> userRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/alert-notification-rules")
            .as(new GenericType<RestResponse<List<AlertNotificationRule>>>() {}.getType());
    return userRestResponse.getResource();
  }

  public static void deleteAlerts(String accountId, String bearerToken, String ruleId) {
    Integer returnCode = Setup.portal()
                             .auth()
                             .oauth2(bearerToken)
                             .queryParam("accountId", accountId)
                             .delete("/alert-notification-rules/" + ruleId)
                             .getStatusCode();
    assertTrue(returnCode == 200);
  }

  public static AlertNotificationRule updateAlert(
      String accountId, String bearerToken, String alertRuleId, AlertNotificationRule alertNotificationRule) {
    RestResponse<AlertNotificationRule> alertRule =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .body(alertNotificationRule, ObjectMapperType.GSON)
            .put("/alert-notification-rules/" + alertRuleId)
            .as(new GenericType<RestResponse<AlertNotificationRule>>() {}.getType());
    return alertRule.getResource();
  }
}
