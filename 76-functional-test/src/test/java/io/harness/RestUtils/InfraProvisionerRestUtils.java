package io.harness.RestUtils;

import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner;

import javax.ws.rs.core.GenericType;

@Singleton
public class InfraProvisionerRestUtils extends AbstractFunctionalTest {
  public InfrastructureProvisioner saveProvisioner(String appId, InfrastructureProvisioner infrastructureProvisioner)
      throws Exception {
    GenericType<RestResponse<TerraformInfrastructureProvisioner>> provisioner =
        new GenericType<RestResponse<TerraformInfrastructureProvisioner>>() {};

    RestResponse<InfrastructureProvisioner> response = Setup.portal()
                                                           .auth()
                                                           .oauth2(bearerToken)
                                                           .queryParam("appId", appId)
                                                           .body(infrastructureProvisioner, ObjectMapperType.GSON)
                                                           .contentType(ContentType.JSON)
                                                           .post("/infrastructure-provisioners")
                                                           .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new Exception(String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public void deleteProvisioner(String appId, String provisionerId) throws Exception {
    RestResponse response = Setup.portal()
                                .auth()
                                .oauth2(bearerToken)
                                .queryParam("appId", appId)
                                .contentType(ContentType.JSON)
                                .delete("/infrastructure-provisioners/" + provisionerId)
                                .as(RestResponse.class);
    if (EmptyPredicate.isNotEmpty(response.getResponseMessages())) {
      throw new Exception(String.valueOf(response.getResponseMessages()));
    }
  }

  public InfrastructureProvisioner getProvisioner(String appId, String provisionerId) throws Exception {
    GenericType<RestResponse<TerraformInfrastructureProvisioner>> provisioner =
        new GenericType<RestResponse<TerraformInfrastructureProvisioner>>() {};

    RestResponse<InfrastructureProvisioner> response = Setup.portal()
                                                           .auth()
                                                           .oauth2(bearerToken)
                                                           .queryParam("appId", appId)
                                                           .contentType(ContentType.JSON)
                                                           .get("/infrastructure-provisioners/" + provisionerId)
                                                           .as(provisioner.getType());
    if (response.getResource() == null) {
      throw new Exception(String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }
}
