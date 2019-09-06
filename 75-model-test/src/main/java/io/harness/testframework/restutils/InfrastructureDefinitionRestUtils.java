package io.harness.testframework.restutils;

import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import io.harness.exception.EmptyRestResponseException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.infra.InfrastructureDefinition;

import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.GenericType;

@Singleton
public class InfrastructureDefinitionRestUtils {
  public static InfrastructureDefinition save(String bearerToken, InfrastructureDefinition infrastructureDefinition) {
    GenericType<RestResponse<InfrastructureDefinition>> infraDefinitionType =
        new GenericType<RestResponse<InfrastructureDefinition>>() {};
    RestResponse<InfrastructureDefinition> response = Setup.portal()
                                                          .auth()
                                                          .oauth2(bearerToken)
                                                          .queryParam("appId", infrastructureDefinition.getAppId())
                                                          .queryParam("envId", infrastructureDefinition.getEnvId())
                                                          .body(infrastructureDefinition, ObjectMapperType.JACKSON_2)
                                                          .contentType(ContentType.JSON)
                                                          .post("/infrastructure-definitions")
                                                          .as(infraDefinitionType.getType());

    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static InfrastructureDefinition update(String bearerToken, InfrastructureDefinition infrastructureDefinition) {
    GenericType<RestResponse<InfrastructureDefinition>> infraDefinitionType =
        new GenericType<RestResponse<InfrastructureDefinition>>() {};
    RestResponse<InfrastructureDefinition> response =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", infrastructureDefinition.getAppId())
            .queryParam("envId", infrastructureDefinition.getEnvId())
            .body(infrastructureDefinition, ObjectMapperType.JACKSON_2)
            .contentType(ContentType.JSON)
            .put("/infrastructure-definitions/" + infrastructureDefinition.getUuid())
            .as(infraDefinitionType.getType());

    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static InfrastructureDefinition get(
      String bearerToken, String accountId, String appId, String envId, String infraDefinitionId) {
    GenericType<RestResponse<InfrastructureDefinition>> infraDefinitionType =
        new GenericType<RestResponse<InfrastructureDefinition>>() {};
    RestResponse<InfrastructureDefinition> restResponse = Setup.portal()
                                                              .auth()
                                                              .oauth2(bearerToken)
                                                              .contentType(ContentType.JSON)
                                                              .queryParam("appId", appId)
                                                              .queryParam("envId", envId)
                                                              .queryParam("routingId", accountId)
                                                              .get("/infrastructure-definitions/" + infraDefinitionId)
                                                              .as(infraDefinitionType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions/" + infraDefinitionId, String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static void delete(
      String bearerToken, String accountId, String appId, String envId, String infraDefinitionId) {
    GenericType<RestResponse> restResponseGenericType = new GenericType<RestResponse>() {};
    RestResponse restResponse = Setup.portal()
                                    .auth()
                                    .oauth2(bearerToken)
                                    .contentType(ContentType.JSON)
                                    .queryParam("appId", appId)
                                    .queryParam("envId", envId)
                                    .queryParam("routingId", accountId)
                                    .delete("/infrastructure-definitions/" + infraDefinitionId)
                                    .as(restResponseGenericType.getType());
  }

  public static List<String> listHosts(String bearerToken, String appId, String envId, String infraDefinitionId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .queryParam("envId", envId)
                                                  .get("/infrastructure-definitions/" + infraDefinitionId + "/hosts")
                                                  .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException("/infrastructure-definitions/" + infraDefinitionId + "/hosts",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static List<String> listInfraDefinitionByService(
      String bearerToken, String accountId, String appId, String serviceId, String envId) {
    GenericType<RestResponse<PageResponse<InfrastructureDefinition>>> restResponseGenericType =
        new GenericType<RestResponse<PageResponse<InfrastructureDefinition>>>() {};
    RestResponse<PageResponse<InfrastructureDefinition>> restResponse = Setup.portal()
                                                                            .auth()
                                                                            .oauth2(bearerToken)
                                                                            .contentType(ContentType.JSON)
                                                                            .queryParam("routingId", accountId)
                                                                            .queryParam("appId", appId)
                                                                            .queryParam("envId", envId)
                                                                            .queryParam("serviceId", serviceId)
                                                                            .get("/infrastructure-definitions/")
                                                                            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions", String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource()
        .getResponse()
        .stream()
        .map(InfrastructureDefinition::getUuid)
        .collect(Collectors.toList());
  }
}
