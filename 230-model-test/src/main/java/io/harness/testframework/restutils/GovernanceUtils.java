/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static io.restassured.RestAssured.given;

import io.harness.rest.RestResponse;

import software.wings.beans.governance.GovernanceConfig;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
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
