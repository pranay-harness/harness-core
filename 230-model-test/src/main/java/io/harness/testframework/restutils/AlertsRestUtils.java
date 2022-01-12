/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.alert.AlertNotificationRule;

import io.restassured.mapper.ObjectMapperType;
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
    assertThat(returnCode == 200).isTrue();
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
