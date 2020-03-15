package software.wings.service.impl.prometheus;

import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.getUnsafeHttpClient;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.PrometheusConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.prometheus.PrometheusRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;

import java.time.OffsetDateTime;

/**
 * Created by rsingh on 3/14/18.
 */
@Singleton
@Slf4j
public class PrometheusDelegateServiceImpl implements PrometheusDelegateService {
  @Inject private DelegateLogService delegateLogService;
  @Inject private RequestExecutor requestExecutor;

  @Override
  public PrometheusMetricDataResponse fetchMetricData(
      PrometheusConfig prometheusConfig, String url, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle("Fetching metric data from " + prometheusConfig.getUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    final Call<PrometheusMetricDataResponse> request = getRestClient(prometheusConfig).fetchMetricData(url);
    return requestExecutor.executeRequest(apiCallLog, request);
  }

  private PrometheusRestClient getRestClient(PrometheusConfig prometheusConfig) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(prometheusConfig.getUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(getUnsafeHttpClient(prometheusConfig.getUrl()))
                                  .build();
    return retrofit.create(PrometheusRestClient.class);
  }
}
