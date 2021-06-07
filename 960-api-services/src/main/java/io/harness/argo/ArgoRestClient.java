package io.harness.argo;

import com.google.gson.JsonObject;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.argo.beans.ArgoToken;
import io.harness.argo.beans.UsernamePassword;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

@OwnedBy(HarnessTeam.CDP)
public interface ArgoRestClient {
  @POST("api/v1/session") Call<ArgoToken> fetchToken(@Body UsernamePassword usernamePassword);

  @GET("api/v1/applications/{appName}") Call<JsonObject> fetchApp(@Header("Authorization") String bearerAuthHeader, @Path(value = "appName") String appName);
}
