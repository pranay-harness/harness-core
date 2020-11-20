package io.harness.testframework.restutils;

import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.governance.GovernanceConfig;

import javax.ws.rs.core.GenericType;

@Singleton
public class GovernanceRestUtils {
  public static GovernanceConfig checkGovernanceStatus(String bearerToken, String accountId) {
    GenericType<RestResponse<GovernanceConfig>> type = new GenericType<RestResponse<GovernanceConfig>>() {};

    RestResponse<GovernanceConfig> savedGovernanceResponse = Setup.portal()
                                                                 .auth()
                                                                 .oauth2(bearerToken)
                                                                 .queryParam("accountId", accountId)
                                                                 .contentType(ContentType.JSON)
                                                                 .get("/compliance-config/" + accountId)
                                                                 .as(type.getType());

    if (savedGovernanceResponse.getResource() == null) {
      throw new WingsException(String.valueOf(savedGovernanceResponse.getResponseMessages()));
    }

    return savedGovernanceResponse.getResource();
  }

  public static void setDeploymentFreeze(String bearerToken, String accountId, boolean freeze) {
    GenericType<RestResponse<GovernanceConfig>> returnType = new GenericType<RestResponse<GovernanceConfig>>() {};
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder().accountId(accountId).deploymentFreeze(freeze).build();
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("routingId", accountId)
        .contentType(ContentType.JSON)
        .body(governanceConfig, ObjectMapperType.GSON)
        .put("/compliance-config/" + accountId)
        .as(returnType.getType());
  }
}
