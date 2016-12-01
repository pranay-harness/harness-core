package software.wings.helpers.ext.bamboo;

import com.fasterxml.jackson.databind.JsonNode;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by anubhaw on 11/29/16.
 */
public interface BambooRestClient {
  @GET("project.json?authType=basic&expand=projects.project.plans.plan.stages.stage.plans.plan")
  Call<JsonNode> listProjectsWithJobDetails(@Header("Authorization") String authorization);

  @GET("result/{jobKey}.json?authType=basic&buildState=Successful&max-result=1")
  Call<JsonNode> lastSuccessfulBuild(@Header("Authorization") String authorization, @Path("jobKey") String jobKey);

  @GET("result/{jobKey}.json?authType=basic&buildState=Successful")
  Call<JsonNode> listBuildsForJob(
      @Header("Authorization") String authorization, @Path("jobKey") String jobKey, @Query("max-result") int maxResult);

  @GET("result/{jobKey}/{buildNo}.json?authType=basic&expand=artifacts")
  Call<JsonNode> getBuildArtifacts(
      @Header("Authorization") String authorization, @Path("jobKey") String jobKey, @Path("buildNo") String buildNo);
}
