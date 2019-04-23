package io.harness.restutils;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import software.wings.beans.Account;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;

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

  public static JsonPath configureInfraMapping(String bearerToken, Account account, String applicationId,
      String environmentId, AwsInfrastructureMapping infrastructureMapping) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", account.getUuid())
                            .queryParam("appId", applicationId)
                            .queryParam("envId", environmentId)
                            .body(infrastructureMapping, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/infrastructure-mappings")
                            .jsonPath();

    return response;
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
      return data.get("uuid").toString();
    }
    return null;
  }
}