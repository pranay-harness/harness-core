package software.wings.helpers.ext.newrelic;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import software.wings.service.impl.newrelic.NewRelicApplicationInstancesResponse;
import software.wings.service.impl.newrelic.NewRelicApplicationsResponse;
import software.wings.service.impl.newrelic.NewRelicMetricDataResponse;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicRestClient {
  /**
   * Lists all the applications of new relic
   *
   * @return the call
   */
  @GET("v2/applications.json") Call<NewRelicApplicationsResponse> listAllApplications();

  @GET("v2/applications/{applicationId}/instances.json")
  Call<NewRelicApplicationInstancesResponse> listAppInstances(@Path("applicationId") long newRelicAppId);

  @GET()
  Call<NewRelicMetricDataResponse> getRawMetricData(
      @Url String url, @Query("names[]") String metricName, @Query("from") String fromTime, @Query("to") String toTime);
}
