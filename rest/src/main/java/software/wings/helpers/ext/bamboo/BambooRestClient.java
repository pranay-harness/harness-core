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
  /**
   * List projects with job details call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("project.json?authType=basic&expand=projects.project.plans.plan.stages.stage.plans.plan")
  Call<JsonNode> listProjectsWithJobDetails(@Header("Authorization") String authorization);

  /**
   * Last successful build call.
   *
   * @param authorization the authorization
   * @param jobKey        the job key
   * @return the call
   */
  @GET("result/{jobKey}.json?authType=basic&buildState=Successful&max-result=1")
  Call<JsonNode> lastSuccessfulBuild(@Header("Authorization") String authorization, @Path("jobKey") String jobKey);

  /**
   * List builds for job call.
   *
   * @param authorization the authorization
   * @param jobKey        the job key
   * @param maxResult     the max result
   * @return the call
   */
  @GET("result/{jobKey}.json?authType=basic&buildState=Successful")
  Call<JsonNode> listBuildsForJob(
      @Header("Authorization") String authorization, @Path("jobKey") String jobKey, @Query("max-result") int maxResult);

  /**
   * Gets build artifacts.
   *
   * @param authorization the authorization
   * @param jobKey        the job key
   * @param buildNo       the build no
   * @return the build artifacts
   */
  @GET("result/{jobKey}/{buildNo}.json?authType=basic&expand=artifacts")
  Call<JsonNode> getBuildArtifacts(
      @Header("Authorization") String authorization, @Path("jobKey") String jobKey, @Path("buildNo") String buildNo);
}
