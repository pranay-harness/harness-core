package io.harness.testframework.restutils;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.exception.EmptyRestResponseException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;

import com.google.inject.Singleton;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import javax.ws.rs.core.GenericType;

@Singleton
public class InfraProvisionerRestUtils {
  public static InfrastructureProvisioner saveProvisioner(String appId, String bearerToken,
      InfrastructureProvisioner infrastructureProvisioner) throws EmptyRestResponseException {
    GenericType<RestResponse<InfrastructureProvisioner>> provisioner =
        new GenericType<RestResponse<InfrastructureProvisioner>>() {};

    RestResponse<InfrastructureProvisioner> response = Setup.portal()
                                                           .auth()
                                                           .oauth2(bearerToken)
                                                           .queryParam("appId", appId)
                                                           .body(infrastructureProvisioner, ObjectMapperType.GSON)
                                                           .contentType(ContentType.JSON)
                                                           .post("/infrastructure-provisioners")
                                                           .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-provisioners/", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static void deleteProvisioner(String appId, String bearerToken, String provisionerId)
      throws EmptyRestResponseException {
    RestResponse response = Setup.portal()
                                .auth()
                                .oauth2(bearerToken)
                                .queryParam("appId", appId)
                                .contentType(ContentType.JSON)
                                .delete("/infrastructure-provisioners/" + provisionerId)
                                .as(RestResponse.class);
    if (hasSome(response.getResponseMessages())) {
      throw new EmptyRestResponseException(
          "/infrastructure-provisioners/" + provisionerId, String.valueOf(response.getResponseMessages()));
    }
  }

  public static InfrastructureProvisioner getProvisioner(String appId, String bearerToken, String provisionerId)
      throws EmptyRestResponseException {
    GenericType<RestResponse<InfrastructureProvisioner>> provisioner =
        new GenericType<RestResponse<InfrastructureProvisioner>>() {};

    RestResponse<InfrastructureProvisioner> response = Setup.portal()
                                                           .auth()
                                                           .oauth2(bearerToken)
                                                           .queryParam("appId", appId)
                                                           .contentType(ContentType.JSON)
                                                           .get("/infrastructure-provisioners/" + provisionerId)
                                                           .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-provisioners/" + provisionerId, String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static InfrastructureProvisioner updateProvisioner(String appId, String bearerToken,
      InfrastructureProvisioner infrastructureProvisioner, String provisionerId) throws EmptyRestResponseException {
    GenericType<RestResponse<InfrastructureProvisioner>> provisioner =
        new GenericType<RestResponse<InfrastructureProvisioner>>() {};
    RestResponse<InfrastructureProvisioner> response = Setup.portal()
                                                           .auth()
                                                           .oauth2(bearerToken)
                                                           .queryParam("appId", appId)
                                                           .body(infrastructureProvisioner, ObjectMapperType.GSON)
                                                           .contentType(ContentType.JSON)
                                                           .put("/infrastructure-provisioners/" + provisionerId)
                                                           .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-provisioners/" + provisionerId, String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static List<String> getTerraformTargets(
      String accountId, String appId, String bearerToken, String provisionerid) throws EmptyRestResponseException {
    GenericType<RestResponse<List<String>>> provisioner = new GenericType<RestResponse<List<String>>>() {};
    RestResponse<List<String>> response = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("appId", appId)
                                              .queryParam("accountId", accountId)
                                              .queryParam("provisionerId", provisionerid)
                                              .contentType(ContentType.JSON)
                                              .get("/infrastructure-provisioners/terraform-targets")
                                              .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "infrastructure-provisioners/terraform-targets", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static List<NameValuePair> getTerraformVariables(String accountId, String appId, String bearerToken,
      String scmSettingId, String branch, String path, String repoName) throws EmptyRestResponseException {
    GenericType<RestResponse<List<NameValuePair>>> provisioner =
        new GenericType<RestResponse<List<NameValuePair>>>() {};

    RequestSpecification requestSpecification = Setup.portal()
                                                    .auth()
                                                    .oauth2(bearerToken)
                                                    .queryParam("appId", appId)
                                                    .queryParam("accountId", accountId)
                                                    .queryParam("sourceRepoSettingId", scmSettingId)
                                                    .queryParam("branch", branch)
                                                    .queryParam("path", path)
                                                    .contentType(ContentType.JSON);

    if (repoName != null) {
      requestSpecification.queryParam("repoName", repoName);
    }
    RestResponse<List<NameValuePair>> response = requestSpecification
                                                     .get("/infrastructure-provisioners/terraform"
                                                         + "-variables")
                                                     .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "infrastructure-provisioners/terraform-variable", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }
}
