package io.harness.RestUtils;

import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import software.wings.beans.artifact.Artifact;

import javax.ws.rs.core.GenericType;

@Singleton
public class ArtifactRestUtil extends AbstractFunctionalTest {
  public Artifact getArtifact(String appId, String envId, String serviceId) {
    GenericType<RestResponse<PageResponse<Artifact>>> workflowType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};
    RestResponse<PageResponse<Artifact>> savedArtifactResponse = Setup.portal()
                                                                     .auth()
                                                                     .oauth2(bearerToken)
                                                                     .queryParam("appId", appId)
                                                                     .queryParam("envId", envId)
                                                                     .queryParam("serviceId", serviceId)
                                                                     .queryParam("search[0][field]", "status")
                                                                     .queryParam("search[0][op]", "IN")
                                                                     .queryParam("search[0][value]", "READY")
                                                                     .queryParam("search[0][value]", "APPROVED")
                                                                     .contentType(ContentType.JSON)
                                                                     .get("/artifacts")
                                                                     .as(workflowType.getType());

    // TODO change this to awaitaility
    if (savedArtifactResponse.getResource().isEmpty()) {
      return getArtifact(appId, envId, serviceId);
    }
    return savedArtifactResponse.getResource().getResponse().get(0);
  }
}
