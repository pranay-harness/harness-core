package software.wings.service.impl.dynatrace;

import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.getUnsafeHttpClient;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DynaTraceConfig;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.dynatrace.DynaTraceRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.DynatraceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Created by rsingh on 1/29/18.
 */
@Singleton
@Slf4j
public class DynaTraceDelegateServiceImpl implements DynaTraceDelegateService {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Inject private RequestExecutor requestExecutor;

  @Override
  public boolean validateConfig(DynaTraceConfig dynaTraceConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    final Call<Object> request = getDynaTraceRestClient(dynaTraceConfig)
                                     .listTimeSeries(getHeaderWithCredentials(dynaTraceConfig, encryptedDataDetails));
    requestExecutor.executeRequest(request);
    return true;
  }

  @Override
  public DynaTraceMetricDataResponse fetchMetricData(DynaTraceConfig dynaTraceConfig,
      DynaTraceMetricDataRequest dataRequest, List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle(
        "Fetching metric data for " + dataRequest.getTimeseriesId() + " from " + dynaTraceConfig.getDynaTraceUrl());
    final Call<DynaTraceMetricDataResponse> request =
        getDynaTraceRestClient(dynaTraceConfig)
            .fetchMetricData(getHeaderWithCredentials(dynaTraceConfig, encryptedDataDetails), dataRequest);
    return requestExecutor.executeRequest(apiCallLog, request);
  }

  @Override
  public List<DynaTraceMetricDataResponse> getMetricsWithDataForNode(DynaTraceConfig config,
      List<EncryptedDataDetail> encryptionDetails, DynaTraceSetupTestNodeData setupTestNodeData,
      ThirdPartyApiCallLog thirdPartyApiCallLog) {
    final List<DynaTraceMetricDataResponse> metricDataResponses = new ArrayList<>();

    List<Callable<DynaTraceMetricDataResponse>> callables = new ArrayList<>();
    for (DynaTraceTimeSeries timeSeries : Lists.newArrayList(DynaTraceTimeSeries.REQUEST_PER_MINUTE)) {
      callables.add(() -> {
        DynaTraceMetricDataRequest dataRequest =
            DynaTraceMetricDataRequest.builder()
                .timeseriesId(timeSeries.getTimeseriesId())
                .aggregationType(timeSeries.getAggregationType())
                .percentile(timeSeries.getPercentile())
                .startTimestamp(setupTestNodeData.getFromTime())
                .endTimestamp(setupTestNodeData.getToTime())
                .entities(setupTestNodeData.getServiceEntityId() == null
                        ? null
                        : Collections.singleton(setupTestNodeData.getServiceEntityId()))
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
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(dynaTraceConfig.getDynaTraceUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(getUnsafeHttpClient(dynaTraceConfig.getDynaTraceUrl()))
                                  .build();
    return retrofit.create(DynaTraceRestClient.class);
  }

  private String getHeaderWithCredentials(
      DynaTraceConfig dynaTraceConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(dynaTraceConfig, encryptionDetails);
    return "Api-Token " + new String(dynaTraceConfig.getApiToken());
  }
}
