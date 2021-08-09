package software.wings.helpers.ext.prometheus;

import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by rsingh on 1/29/18.
 */
public interface PrometheusRestClient {
  @GET Call<PrometheusMetricDataResponse> fetchMetricData(@Url String url);
}
