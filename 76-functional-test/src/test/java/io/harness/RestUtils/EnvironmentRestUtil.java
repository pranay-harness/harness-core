package io.harness.RestUtils;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;

import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.GenericType;

@Singleton
public class EnvironmentRestUtil extends AbstractFunctionalTest {
  /**
   *
   * @param applicationId
   * @param environment
   * @return Environment details
   */
  public Environment createEnvironment(String applicationId, Environment environment) {
    GenericType<RestResponse<Environment>> environmentType = new GenericType<RestResponse<Environment>>() {};

    RestResponse<Environment> savedApplicationResponse = Setup.portal()
                                                             .auth()
                                                             .oauth2(bearerToken)
                                                             .queryParam("accountId", getAccount().getUuid())
                                                             .queryParam("appId", applicationId)
                                                             .body(environment, ObjectMapperType.GSON)
                                                             .contentType(ContentType.JSON)
                                                             .post("/environments")
                                                             .as(environmentType.getType());

    return savedApplicationResponse.getResource();
  }

  public GcpKubernetesInfrastructureMapping configureInfraMapping(
      String applicationId, String environmentId, GcpKubernetesInfrastructureMapping infrastructureMapping) {
    GenericType<RestResponse<GcpKubernetesInfrastructureMapping>> infraMappingType =
        new GenericType<RestResponse<GcpKubernetesInfrastructureMapping>>() {};

    RestResponse<GcpKubernetesInfrastructureMapping> savedApplicationResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", getAccount().getUuid())
            .queryParam("appId", applicationId)
            .queryParam("envId", environmentId)
            .body(infrastructureMapping, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/infrastructure-mappings")
            .as(infraMappingType.getType());
    return savedApplicationResponse.getResource();
  }

  public String getServiceTemplateId(String applicationId, String environmentId) {
    JsonPath jsonPath = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", getAccount().getUuid())
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