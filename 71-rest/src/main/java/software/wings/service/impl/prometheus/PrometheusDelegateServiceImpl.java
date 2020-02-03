package software.wings.service.impl.prometheus;

import static software.wings.common.VerificationConstants.URL_STRING;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.getUnsafeHttpClient;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.PrometheusConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.prometheus.PrometheusRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Created by rsingh on 3/14/18.
 */
@Singleton
@Slf4j
public class PrometheusDelegateServiceImpl implements PrometheusDelegateService {
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(PrometheusConfig prometheusConfig) throws IOException {
    try {
      final Call<PrometheusMetricDataResponse> request =
          getRestClient(prometheusConfig).fetchMetricData("api/v1/query?query=up");
      final Response<PrometheusMetricDataResponse> response = request.execute();
      if (response.isSuccessful()) {
        return true;
      } else {
        logger.error("Request not successful. Reason: {}", response);
        throw new IllegalArgumentException(response.errorBody().string());
      }
    } catch (Exception e) {
      throw new WingsException("Could not validate prometheus server. " + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public PrometheusMetricDataResponse fetchMetricData(
      PrometheusConfig prometheusConfig, String url, ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle("Fetching metric data from " + prometheusConfig.getUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    final Call<PrometheusMetricDataResponse> request = getRestClient(prometheusConfig).fetchMetricData(url);
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(URL_STRING)
                                     .value(request.request().url().toString())
                                     .type(FieldType.URL)
                                     .build());
    final Response<PrometheusMetricDataResponse> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(prometheusConfig.getAccountId(), apiCallLog);
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}, url: {}", response, url);
      apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(prometheusConfig.getAccountId(), apiCallLog);
      throw new WingsException("Unsuccessful response while fetching data from Prometheus. Error code: "
          + response.code() + ". Error: " + response.errorBody());
    }
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
