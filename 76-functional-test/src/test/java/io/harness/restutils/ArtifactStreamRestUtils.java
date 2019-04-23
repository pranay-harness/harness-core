package io.harness.restutils;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import software.wings.beans.Account;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;

import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.GenericType;

public class ArtifactStreamRestUtils {
  /**
   *
   * @param appId
   * @param environmentId
   * @param serviceId
   * @return artifact Stream Id
   */
  public static String getArtifactStreamId(String bearerToken, String appId, String environmentId, String serviceId) {
    // TODO : Change this to Matcher pattern which is written by swamy
    int i = 0;
    while (i < 5) {
      JsonPath jsonPath = Setup.portal()
                              .auth()
                              .oauth2(bearerToken)
                              .queryParam("appId", appId)
                              .queryParam("envId", environmentId)
                              .queryParam("serviceId", serviceId)
                              .contentType(ContentType.JSON)
                              .get("/artifacts")
                              .getBody()
                              .jsonPath();

      ArrayList<HashMap<String, String>> hashMaps =
          (ArrayList<HashMap<String, String>>) jsonPath.getMap("resource").get("response");
      if (hashMaps.size() == 0) {
        i++;
        try {
          Thread.sleep(1000 * 5);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        continue;
      }
      for (HashMap<String, String> data : hashMaps) {
        return data.get("uuid");
      }
    }
    return null;
  }

  /**
   *
   * @param appId
   * @param artifactSource
   * @return Docker Artifact steam response
   */
  public static DockerArtifactStream configureDockerArtifactStream(
      String bearerToken, Account account, String appId, DockerArtifactStream artifactSource) {
    GenericType<RestResponse<DockerArtifactStream>> artifactStreamType =
        new GenericType<RestResponse<DockerArtifactStream>>() {};

    RestResponse<DockerArtifactStream> savedServiceResponse = Setup.portal()
                                                                  .auth()
                                                                  .oauth2(bearerToken)
                                                                  .queryParam("accountId", account.getUuid())
                                                                  .queryParam("appId", appId)
                                                                  .body(artifactSource, ObjectMapperType.GSON)
                                                                  .contentType(ContentType.JSON)
                                                                  .post("/artifactstreams")
                                                                  .as(artifactStreamType.getType());

    return savedServiceResponse.getResource();
  }

  /**
   *
   * @param appId
   * @param artifactStream
   * @return Docker Artifact steam response
   */
  public static JsonPath configureArtifactory(String bearerToken, String appId, ArtifactStream artifactStream) {
    JsonPath savedServiceResponse = Setup.portal()
                                        .auth()
                                        .oauth2(bearerToken)
                                        .queryParam("appId", appId)
                                        .body(artifactStream, ObjectMapperType.GSON)
                                        .contentType(ContentType.JSON)
                                        .post("/artifactstreams")
                                        .jsonPath();
    return savedServiceResponse;
  }
}