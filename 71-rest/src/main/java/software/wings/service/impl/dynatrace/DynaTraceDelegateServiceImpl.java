package software.wings.service.impl.dynatrace;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.network.Http;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DynaTraceConfig;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.dynatrace.DynaTraceRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.DynatraceState;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Created by rsingh on 1/29/18.
 */
public class DynaTraceDelegateServiceImpl implements DynaTraceDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(DynaTraceDelegateServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private DataCollectionExecutorService dataCollectionService;

  @Override
  public boolean validateConfig(DynaTraceConfig dynaTraceConfig) throws IOException {
    final Call<Object> request =
        getDynaTraceRestClient(dynaTraceConfig)
            .listTimeSeries(getHeaderWithCredentials(dynaTraceConfig, Collections.emptyList()));
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return true;
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(response.errorBody().string());
    }
  }

  @Override
  public DynaTraceMetricDataResponse fetchMetricData(DynaTraceConfig dynaTraceConfig,
      DynaTraceMetricDataRequest dataRequest, List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle("Fetching metric data from " + dynaTraceConfig.getDynaTraceUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .value(dynaTraceConfig.getDynaTraceUrl())
                                     .type(FieldType.URL)
                                     .build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("payload")
                                     .value(JsonUtils.asJson(dataRequest))
                                     .type(FieldType.JSON)
                                     .build());
    final Call<DynaTraceMetricDataResponse> request =
        getDynaTraceRestClient(dynaTraceConfig)
            .fetchMetricData(getHeaderWithCredentials(dynaTraceConfig, encryptedDataDetails), dataRequest);
    final Response<DynaTraceMetricDataResponse> response;
    try {
      response = request.execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(dynaTraceConfig.getAccountId(), apiCallLog);
      throw new WingsException("Unsuccessful response while fetching data from DynaTrace. Error message: "
          + e.getMessage() + " Request: " + dynaTraceConfig.getDynaTraceUrl());
    }
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(dynaTraceConfig.getAccountId(), apiCallLog);
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}, request: {}", response, dataRequest);
      apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(dynaTraceConfig.getAccountId(), apiCallLog);
      throw new WingsException("Unsuccessful response while fetching data from Dynatrace. Error code: "
          + response.code() + ". Error: " + response.errorBody());
    }
  }

  @Override
  public List<DynaTraceMetricDataResponse> getMetricsWithDataForNode(DynaTraceConfig config,
      List<EncryptedDataDetail> encryptionDetails, DynaTraceSetupTestNodeData setupTestNodeData,
      ThirdPartyApiCallLog thirdPartyApiCallLog) {
    final List<DynaTraceMetricDataResponse> metricDataResponses = new ArrayList<>();
    List<Callable<DynaTraceMetricDataResponse>> callables = new ArrayList<>();
    for (DynaTraceTimeSeries timeSeries : Lists.newArrayList(DynaTraceTimeSeries.values())) {
      callables.add(() -> {
        DynaTraceMetricDataRequest dataRequest = DynaTraceMetricDataRequest.builder()
                                                     .timeseriesId(timeSeries.getTimeseriesId())
                                                     .entities(setupTestNodeData.getServiceMethods())
                                                     .aggregationType(timeSeries.getAggregationType())
                                                     .percentile(timeSeries.getPercentile())
                                                     .startTimestamp(setupTestNodeData.getFromTime())
                                                     .endTimestamp(setupTestNodeData.getToTime())
                                                     .build();

        DynaTraceMetricDataResponse metricDataResponse =
            fetchMetricData(config, dataRequest, encryptionDetails, thirdPartyApiCallLog);
        metricDataResponse.getResult().setHost(DynatraceState.TEST_HOST_NAME);
        return metricDataResponse;
      });
    }
    List<Optional<DynaTraceMetricDataResponse>> results = dataCollectionService.executeParrallel(callables);
    results.forEach(result -> {
      if (result.isPresent()) {
        metricDataResponses.add(result.get());
      }
    });

    return metricDataResponses;
  }

  private DynaTraceRestClient getDynaTraceRestClient(final DynaTraceConfig dynaTraceConfig) {
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(dynaTraceConfig.getDynaTraceUrl())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(Http.getOkHttpClientWithNoProxyValueSet(dynaTraceConfig.getDynaTraceUrl()).build())
            .build();
    return retrofit.create(DynaTraceRestClient.class);
  }

  private String getHeaderWithCredentials(
      DynaTraceConfig dynaTraceConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(dynaTraceConfig, encryptionDetails);
    return "Api-Token " + new String(dynaTraceConfig.getApiToken());
  }
}
