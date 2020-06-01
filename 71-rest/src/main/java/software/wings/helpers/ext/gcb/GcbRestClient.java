package software.wings.helpers.ext.gcb;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.RepoSource;

@OwnedBy(CDC)
public interface GcbRestClient {
  String baseUrl = "https://cloudbuild.googleapis.com/";

  @POST("v1/projects/{projectId}/triggers/{triggerId}:run")
  Call<BuildOperationDetails> runTrigger(@Header("Authorization") String bearerAuthHeader,
      @Path(value = "projectId") String projectID, @Path(value = "triggerId") String triggerID,
      @Body RepoSource repoSource);

  @GET("v1/projects/{projectId}/builds/{buildId}")
  Call<GcbBuildDetails> getBuild(@Header("Authorization") String bearerAuthHeader,
      @Path(value = "projectId") String projectId, @Path(value = "buildId") String buildId);

  @POST("v1/projects/{projectId}/builds")
  Call<BuildOperationDetails> createBuild(@Header("Authorization") String bearerAuthHeader,
      @Path(value = "projectId", encoded = true) String projectId, @Body GcbBuildDetails buildParams);
}
