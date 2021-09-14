/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.functional.multiartifact;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.artifact.Artifact;

import io.restassured.http.ContentType;
import javax.ws.rs.core.GenericType;

public class MultiArtifactTestUtils {
  public static Artifact collectArtifact(String bearerToken, String accountId, String artifactStreamId) {
    GenericType<RestResponse<PageResponse<Artifact>>> workflowType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};
    RestResponse<PageResponse<Artifact>> savedArtifactResponse = Setup.portal()
                                                                     .auth()
                                                                     .oauth2(bearerToken)
                                                                     .queryParam("accountId", accountId)
                                                                     .queryParam("artifactStreamId", artifactStreamId)
                                                                     .queryParam("search[0][field]", "status")
                                                                     .queryParam("search[0][op]", "IN")
                                                                     .queryParam("search[0][value]", "READY")
                                                                     .queryParam("search[0][value]", "APPROVED")
                                                                     .contentType(ContentType.JSON)
                                                                     .get("/artifacts/v2")
                                                                     .as(workflowType.getType());

    return (savedArtifactResponse != null && savedArtifactResponse.getResource() != null
               && savedArtifactResponse.getResource().getResponse() != null
               && savedArtifactResponse.getResource().getResponse().size() > 0)
        ? savedArtifactResponse.getResource().getResponse().get(0)
        : null;
  }
}
