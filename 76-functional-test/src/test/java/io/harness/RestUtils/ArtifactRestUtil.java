package io.harness.RestUtils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.time.Duration.ofMinutes;
import static software.wings.beans.artifact.Artifact.ARTIFACT_STREAM_ID_KEY;

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
  public Artifact waitAndFetchArtifactByArtifactStream(String appId, String artifactStreamId) {
    GenericType<RestResponse<PageResponse<Artifact>>> artifactType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};

    final long start = System.currentTimeMillis();
    while (true) {
      RestResponse<PageResponse<Artifact>> savedArtifactResponse =
          Setup.portal()
              .auth()
              .oauth2(bearerToken)
              .queryParam("appId", appId)
              .queryParam("search[0][field]", ARTIFACT_STREAM_ID_KEY)
              .queryParam("search[0][op]", "EQ")
              .queryParam("search[0][value]", artifactStreamId)
              .queryParam("search[1][field]", "status")
              .queryParam("search[1][op]", "IN")
              .queryParam("search[1][value]", "READY")
              .queryParam("search[1][value]", "APPROVED")
              .contentType(ContentType.JSON)
              .get("/artifacts")
              .as(artifactType.getType());

      if (isNotEmpty(savedArtifactResponse.getResource())) {
        return savedArtifactResponse.getResource().getResponse().get(0);
      }

      if (System.currentTimeMillis() - start > ofMinutes(2).toMillis()) {
        throw new WingsException("No artifact in the collection");
      }
    }
  }
}
