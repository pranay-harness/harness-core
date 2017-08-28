package software.wings.helpers.ext.newrelic;

import retrofit2.Call;
import retrofit2.http.GET;
import software.wings.service.impl.newrelic.NewRelicApplicationsResponse;

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
}
