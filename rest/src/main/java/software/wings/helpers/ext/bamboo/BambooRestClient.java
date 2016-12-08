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
   * @param planKey       the plan key
   * @return the call
   */
  @GET("plan/{planKey}.json?authType=basic&expand=stages.stage.plans.plan")
  Call<JsonNode> listPlanWithJobDetails(@Header("Authorization") String authorization, @Path("planKey") String planKey);

  /**
   * List project plans call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("plan.json?authType=basic") Call<JsonNode> listProjectPlans(@Header("Authorization") String authorization);

  /**
   * Last successful build call.
   *
   * @param authorization the authorization
   * @param jobKey        the job key
   * @return the call
   */
  @GET("result/{jobKey}.json?authType=basic&buildState=Successful&max-result=1&expand=results.result")
  Call<JsonNode> lastSuccessfulBuildForJob(
      @Header("Authorization") String authorization, @Path("jobKey") String jobKey);

  /**
   * List builds for job call.
   *
   * @param authorization the authorization
   * @param planKey       the plan key
   * @param maxResult     the max result
   * @return the call
   */
  @GET("result/{planKey}.json?authType=basic&buildState=Successful&expand=results.result")
  Call<JsonNode> listBuildsForJob(@Header("Authorization") String authorization, @Path("planKey") String planKey,
      @Query("max-result") int maxResult);

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
