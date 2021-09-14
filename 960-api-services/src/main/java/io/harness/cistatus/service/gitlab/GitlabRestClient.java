/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cistatus.service.gitlab;

import io.harness.cistatus.StatusCreationResponse;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GitlabRestClient {
  @POST("v4/projects/{id}/statuses/{sha}/")
  Call<StatusCreationResponse> createStatus(@Header("Authorization") String authorization, @Path("id") String id,
      @Path("sha") String sha, @Query("state") String state, @Query("context") String context,
      @Query("description") String description);
}
