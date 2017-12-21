package software.wings.helpers.ext.newrelic;

import retrofit2.Call;
import retrofit2.http.*;
import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.service.impl.newrelic.NewRelicApplicationInstancesResponse;
import software.wings.service.impl.newrelic.NewRelicApplicationsResponse;
import software.wings.service.impl.newrelic.NewRelicMetricDataResponse;
import software.wings.service.impl.newrelic.NewRelicMetricResponse;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicRestClient {
  /**
   * Lists all the applications of new relic
   *
   * @return the call
   */
  @GET("v2/applications.json")
  Call<NewRelicApplicationsResponse> listAllApplications(@Query("page") int durantionInMinutes);

  @GET("v2/applications/{applicationId}/instances.json")
  Call<NewRelicApplicationInstancesResponse> listAppInstances(@Path("applicationId") long newRelicAppId);

  @GET()
  Call<NewRelicMetricDataResponse> getRawMetricData(
      @Url String url, @Query("from") String fromTime, @Query("to") String toTime);

  @GET("v2/applications/{applicationId}/metrics.json?name=WebTransaction/")
  Call<NewRelicMetricResponse> listMetricNames(@Path("applicationId") long newRelicAppId);

  @POST() Call<Object> postDeploymentMarker(@Url String url, @Body NewRelicDeploymentMarkerPayload body);
}
