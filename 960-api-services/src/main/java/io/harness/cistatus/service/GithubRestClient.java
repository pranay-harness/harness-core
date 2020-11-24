package io.harness.cistatus.service;

import io.harness.cistatus.GithubAppTokenCreationResponse;
import io.harness.cistatus.GithubStatusCreationResponse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GithubRestClient {
  @POST("app/installations/{installation_id}/access_tokens")
  @Headers("Accept: application/vnd.github.v3+json")
  Call<GithubAppTokenCreationResponse> createAccessToken(
      @Header("Authorization") String authorization, @Path("installation_id") String installationId);

  @POST("repos/{owner}/{repo}/statuses/{sha}")
  @Headers("Accept: application/vnd.github.v3+json")
  Call<GithubStatusCreationResponse> createStatus(@Header("Authorization") String authorization,
      @Path("owner") String owner, @Path("repo") String repo, @Path("sha") String sha,
      @Body Map<String, Object> parameters);
}
