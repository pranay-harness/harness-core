/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;

import com.google.inject.Singleton;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.GenericType;

@Singleton
public class EnvironmentRestUtils {
  /**
   *
   * @param applicationId
   * @param environment
   * @return Environment details
   */
  public static Environment createEnvironment(
      String bearerToken, Account account, String applicationId, Environment environment) {
    GenericType<RestResponse<Environment>> environmentType = new GenericType<RestResponse<Environment>>() {};

    RestResponse<Environment> savedApplicationResponse = Setup.portal()
                                                             .auth()
                                                             .oauth2(bearerToken)
                                                             .queryParam("accountId", account.getUuid())
                                                             .queryParam("appId", applicationId)
                                                             .body(environment, ObjectMapperType.GSON)
                                                             .contentType(ContentType.JSON)
                                                             .post("/environments")
                                                             .as(environmentType.getType());

    return savedApplicationResponse.getResource();
  }

  public static Environment updateEnvironment(
      String bearerToken, String accountId, String applicationId, Environment environment) {
    GenericType<RestResponse<Environment>> environmentType = new GenericType<RestResponse<Environment>>() {};

    RestResponse<Environment> environmentRestResponse = Setup.portal()
                                                            .auth()
                                                            .oauth2(bearerToken)
                                                            .queryParam("appId", applicationId)
                                                            .body(environment, ObjectMapperType.GSON)
                                                            .contentType(ContentType.JSON)
                                                            .put("/environments/" + environment.getUuid())
                                                            .as(environmentType.getType());

    return environmentRestResponse.getResource();
  }

  public static int deleteEnvironment(String bearerToken, String appId, String accountId, String environmentId) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("appId", appId)
        .contentType(ContentType.JSON)
        .delete("/environments/" + environmentId)
        .statusCode();
  }

  public static GcpKubernetesInfrastructureMapping configureInfraMapping(String bearerToken, Account account,
      String applicationId, String environmentId, GcpKubernetesInfrastructureMapping infrastructureMapping) {
    GenericType<RestResponse<GcpKubernetesInfrastructureMapping>> infraMappingType =
        new GenericType<RestResponse<GcpKubernetesInfrastructureMapping>>() {};

    RestResponse<GcpKubernetesInfrastructureMapping> savedApplicationResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
            .queryParam("appId", applicationId)
            .queryParam("envId", environmentId)
            .body(infrastructureMapping, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/infrastructure-mappings")
            .as(infraMappingType.getType());
    return savedApplicationResponse.getResource();
  }

  public static JsonPath configureInfraMapping(String bearerToken, String accountId, String applicationId,
      String environmentId, GcpKubernetesInfrastructureMapping infrastructureMapping) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("appId", applicationId)
        .queryParam("envId", environmentId)
        .body(infrastructureMapping, ObjectMapperType.GSON)
        .contentType(ContentType.JSON)
        .post("/infrastructure-mappings")
        .jsonPath();
  }

  public static JsonPath configureInfraMapping(String bearerToken, String accountId, String applicationId,
      String environmentId, AwsInfrastructureMapping infrastructureMapping) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("appId", applicationId)
        .queryParam("envId", environmentId)
        .body(infrastructureMapping, ObjectMapperType.GSON)
        .contentType(ContentType.JSON)
        .post("/infrastructure-mappings")
        .jsonPath();
  }

  public static JsonPath configureInfraMapping(String bearerToken, String accountId, String applicationId,
      String environmentId, EcsInfrastructureMapping ecsInfrastructureMapping) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("appId", applicationId)
        .queryParam("envId", environmentId)
        .body(ecsInfrastructureMapping, ObjectMapperType.GSON)
        .contentType(ContentType.JSON)
        .post("/infrastructure-mappings")
        .jsonPath();
  }

  public static String getServiceTemplateId(
      String bearerToken, Account account, String applicationId, String environmentId) {
    JsonPath jsonPath = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", account.getUuid())
                            .queryParam("appId", applicationId)
                            .queryParam("envId", environmentId)
                            .contentType(ContentType.JSON)
                            .get("/service-templates")
                            .getBody()
                            .jsonPath();
    ArrayList<HashMap<String, String>> hashMaps =
        (ArrayList<HashMap<String, String>>) jsonPath.getMap("resource").get("response");
    for (HashMap<String, String> data : hashMaps) {
      if (data != null) {
        return data.get("uuid");
      }
    }
    return null;
  }

  public static String saveInfraMapping(String bearerToken, String accountId, String applicationId,
      String environmentId, PhysicalInfrastructureMapping infrastructureMapping) throws Exception {
    GenericType<RestResponse<PhysicalInfrastructureMapping>> infraMappingType =
        new GenericType<RestResponse<PhysicalInfrastructureMapping>>() {};
    RestResponse<PhysicalInfrastructureMapping> savedApplicationResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .queryParam("appId", applicationId)
            .queryParam("envId", environmentId)
            .body(infrastructureMapping, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/infrastructure-mappings")
            .as(infraMappingType.getType());

    if (savedApplicationResponse.getResource() == null) {
      throw new Exception(String.valueOf(savedApplicationResponse.getResponseMessages()));
    }
    return savedApplicationResponse.getResource().getUuid();
  }
}
