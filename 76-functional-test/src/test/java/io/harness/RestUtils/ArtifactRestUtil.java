package io.harness.RestUtils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.time.Duration.ofMinutes;

import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import software.wings.beans.artifact.Artifact;

import javax.ws.rs.core.GenericType;

@Singleton
public class ArtifactRestUtil extends AbstractFunctionalTest {
  public Artifact getExistingArtifact(String appId, String envId, String serviceId) {
    GenericType<RestResponse<PageResponse<Artifact>>> workflowType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};

    final long start = System.currentTimeMillis();
    while (true) {
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

      if (isNotEmpty(savedArtifactResponse.getResource())) {
        return savedArtifactResponse.getResource().getResponse().get(0);
      }

      if (System.currentTimeMillis() - start > ofMinutes(2).toMillis()) {
        throw new WingsException("No artifact in the collection");
      }
    }
  }
}
