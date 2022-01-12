/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cistatus.service.bitbucket;

import io.harness.cistatus.StatusCreationResponse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface BitbucketRestClient {
  @POST("2.0/repositories/{workspace}/{repo_slug}/commit/{node}/statuses/build/")
  Call<StatusCreationResponse> createStatus(@Header("Authorization") String authorization,
      @Path("workspace") String workspace, @Path("repo_slug") String repo_slug, @Path("node") String node,
      @Body Map<String, Object> parameters);

  @POST("rest/build-status/1.0/commits/{commitId}/")
  Call<StatusCreationResponse> createOnPremStatus(@Header("Authorization") String authorization,
      @Path("commitId") String sha, @Body Map<String, Object> parameters);
}
