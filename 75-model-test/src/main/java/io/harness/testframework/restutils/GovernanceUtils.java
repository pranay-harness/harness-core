package io.harness.testframework.restutils;

import static io.restassured.RestAssured.given;

import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.governance.GovernanceConfig;

import javax.ws.rs.core.GenericType;

public class GovernanceUtils {
  public static void setDeploymentFreeze(String accountId, String bearerToken, boolean freeze) {
    GenericType<RestResponse<GovernanceConfig>> returnType = new GenericType<RestResponse<GovernanceConfig>>() {};
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder().accountId(accountId).deploymentFreeze(freeze).build();
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .contentType(ContentType.JSON)
        .body(governanceConfig, ObjectMapperType.GSON)
        .put("/compliance-config/" + accountId)
        .as(returnType.getType());
  }
}
