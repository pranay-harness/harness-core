package io.harness.checks.buildpulse.client;

import io.harness.checks.buildpulse.dto.TestFlakinessList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BuildPulseClient {
  @GET("/api/repos/{org}/{repo}/tests")
  Call<TestFlakinessList> listFlakyTests(@Path("org") String org, @Path("repo") String repo);
}
