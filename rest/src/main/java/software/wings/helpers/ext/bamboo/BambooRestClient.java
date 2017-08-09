package software.wings.helpers.ext.bamboo;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

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
  @GET("rest/api/latest/plan/{planKey}.json?authType=basic&expand=stages.stage.plans.plan")
  Call<JsonNode> listPlanWithJobDetails(@Header("Authorization") String authorization, @Path("planKey") String planKey);

  /**
   * List project plans call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("rest/api/latest/plan.json?authType=basic")
  Call<JsonNode> listProjectPlans(@Header("Authorization") String authorization);

  /**
   * Last successful build call.
   *
   * @param authorization the authorization
   * @param planKey       the plan key
   * @return the call
   */
  @GET(
      "rest/api/latest/result/{planKey}.json?authType=basic&buildstate=Successful&max-results=1&expand=results.result.artifacts.artifact")
  Call<JsonNode>
  lastSuccessfulBuildForJob(@Header("Authorization") String authorization, @Path("planKey") String planKey);

  /**
   * List builds for job call.
   *
   * @param authorization the authorization
   * @param planKey       the plan key
   * @param maxResult     the max result
   * @return the call
   */
  @GET("rest/api/latest/result/{planKey}.json?authType=basic&buildstate=Successful&expand=results.result")
  Call<JsonNode> listBuildsForJob(@Header("Authorization") String authorization, @Path("planKey") String planKey,
      @Query("max-result") int maxResult);

  /**
   * Gets build artifacts.
   *
   * @param authorization the authorization
   * @param planKey       the plan key
   * @param buildNo       the build no
   * @return the build artifacts
   */
  @GET(
      "rest/api/latest/result/{planKey}-{buildNo}.json?authType=basic&expand=stages.stage.results.result.artifacts.artifact")
  Call<JsonNode>
  getBuildArtifacts(
      @Header("Authorization") String authorization, @Path("planKey") String planKey, @Path("buildNo") String buildNo);

  /**
   * Download artifact call.
   *
   * @param authorization the authorization
   * @param fileUrl       the file url
   * @return the call
   */
  @GET Call<ResponseBody> downloadArtifact(@Header("Authorization") String authorization, @Url String fileUrl);
}
