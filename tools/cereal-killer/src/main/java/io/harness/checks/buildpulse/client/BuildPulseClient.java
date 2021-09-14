/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks.buildpulse.client;

import io.harness.checks.buildpulse.dto.TestFlakinessList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BuildPulseClient {
  @GET("/api/repos/{org}/{repo}/tests")
  Call<TestFlakinessList> listFlakyTests(@Path("org") String org, @Path("repo") String repo);
}
