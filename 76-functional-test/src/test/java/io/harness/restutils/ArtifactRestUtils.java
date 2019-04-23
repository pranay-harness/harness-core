package io.harness.restutils;

import static software.wings.beans.artifact.Artifact.ARTIFACT_STREAM_ID_KEY;

import io.harness.beans.PageResponse;
import io.harness.framework.Retry;
import io.harness.framework.Setup;
import io.harness.framework.matchers.ArtifactMatcher;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import javax.ws.rs.core.GenericType;

public class ArtifactRestUtils {
  public static List<Artifact> fetchArtifactByArtifactStream(
      String bearerToken, String appId, String artifactStreamId) {
    GenericType<RestResponse<PageResponse<Artifact>>> artifactType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};

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

    return savedArtifactResponse.getResource();
  }

  public static Artifact waitAndFetchArtifactByArtfactStream(
      String bearerToken, String appId, String artifactStreamId) {
    Retry retry = new Retry(80, 10000);
    List<Artifact> artifacts = (List<Artifact>) retry.executeWithRetry(
        () -> fetchArtifactByArtifactStream(bearerToken, appId, artifactStreamId), new ArtifactMatcher(), null);
    return artifacts.get(0);
  }
}
