/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
          log.error("Exception thrown:", e);
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
  public static JsonPath configureDockerArtifactStream(
      String bearerToken, String accountId, String appId, DockerArtifactStream artifactSource) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("routingId", accountId)
        .queryParam("appId", appId)
        .body(artifactSource, ObjectMapperType.GSON)
        .contentType(ContentType.JSON)
        .post("/artifactstreams")
        .jsonPath();
  }

  /**
   *
   * @param appId
   * @param artifactStream
   * @return Docker Artifact steam response
   */
  public static JsonPath configureArtifactory(String bearerToken, String appId, ArtifactStream artifactStream) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", appId)
        .body(artifactStream, ObjectMapperType.GSON)
        .contentType(ContentType.JSON)
        .post("/artifactstreams")
        .jsonPath();
  }

  public static Object deleteArtifactStream(String bearerToken, String artifactStreamId, String appId) {
    GenericType<RestResponse> workflowType = new GenericType<RestResponse>() {};
    RestResponse savedResponse = Setup.portal()
                                     .auth()
                                     .oauth2(bearerToken)
                                     .contentType(ContentType.JSON)
                                     .queryParam("appId", appId)
                                     .delete("/artifactstreams/" + artifactStreamId)
                                     .as(workflowType.getType());

    return savedResponse.getResource();
  }
}
