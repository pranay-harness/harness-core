package software.wings.service.impl.prometheus;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.resources.PrometheusResource.renderFetchQueries;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;

import java.util.HashMap;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by Pranjal on 09/02/2018
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class PrometheusAnalysisServiceImpl implements PrometheusAnalysisService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private MLServiceUtils mlServiceUtils;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(PrometheusSetupTestNodeData setupTestNodeData) {
    renderFetchQueries(setupTestNodeData.getTimeSeriesToAnalyze());
    final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
    ThirdPartyApiCallLog apiCallLog = createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid());

    // Request is made without host
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithoutHost =
        getMetricDataByTimeSeries(setupTestNodeData, settingAttribute, apiCallLog, null);

    if (metricDataResponseByTimeSeriesWithoutHost == null) {
      // exception occurred during the call, provider not reachable.
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    PrometheusMetricDataResponse responseDataFromDelegateForServiceLevel =
        metricDataResponseByTimeSeriesWithoutHost.values().iterator().next();
    boolean isLoadPresentForServiceLevel = responseDataFromDelegateForServiceLevel != null
        && isNotEmpty(responseDataFromDelegateForServiceLevel.getData().getResult());

    if (setupTestNodeData.isServiceLevel()) {
      VerificationNodeDataSetupResponse responseForServiceLevel =
          VerificationNodeDataSetupResponse.builder().providerReachable(true).build();
      responseForServiceLevel.setLoadResponse(VerificationLoadResponse.builder()
                                                  .isLoadPresent(isLoadPresentForServiceLevel)
                                                  .loadResponse(responseDataFromDelegateForServiceLevel)
                                                  .build());

      return responseForServiceLevel;
    }

    // make a call with hostname
    String hostName = mlServiceUtils.getHostNameFromExpression(setupTestNodeData);
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithHost =
        getMetricDataByTimeSeries(setupTestNodeData, settingAttribute, apiCallLog, hostName);

    if (isEmpty(metricDataResponseByTimeSeriesWithHost)) {
      // provider was reachable based on first find but something happened in the call with host.
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder()
                            .isLoadPresent(isLoadPresentForServiceLevel)
                            .loadResponse(responseDataFromDelegateForServiceLevel)
                            .build())
          .dataForNode(null)
          .build();
    }
    validateMetricDataResponseByTimeSeriesWithHost(metricDataResponseByTimeSeriesWithHost);
    PrometheusMetricDataResponse responseDataFromDelegateForHost =
        metricDataResponseByTimeSeriesWithHost.values().iterator().next();
    Object responseDataForHost;
    if (responseDataFromDelegateForHost == null || responseDataFromDelegateForHost.getData() == null) {
      responseDataForHost = null;
    } else {
      responseDataForHost = responseDataFromDelegateForHost.getData().getResult();
    }
    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder()
                          .isLoadPresent(isLoadPresentForServiceLevel)
                          .loadResponse(responseDataFromDelegateForServiceLevel)
                          .build())
        .dataForNode(responseDataForHost)
        .build();
  }

  private void validateMetricDataResponseByTimeSeriesWithHost(
      Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithHost) {
    metricDataResponseByTimeSeriesWithHost.forEach((timeSeries, prometheusMetricDataResponse) -> {
      if (prometheusMetricDataResponse != null && prometheusMetricDataResponse.getData() != null
          && prometheusMetricDataResponse.getData().getResult() != null
          && prometheusMetricDataResponse.getData().getResult().size() > 1) {
        throw new VerificationOperationException(ErrorCode.PROMETHEUS_CONFIGURATION_ERROR,
            "Multiple time series values are returned for metric name " + timeSeries.getMetricName()
                + " and group name " + timeSeries.getTxnName()
                + ". Please add more filters to your query to return only one time series.");
      }
    });
  }

  /**
   * Method containing actual implementation for fetch MetricData per timeSeries provided.
   * @param setupTestNodeData
   * @param settingAttribute
   * @param apiCallLog
   * @param hostName
   * @return
   */
  private Map<TimeSeries, PrometheusMetricDataResponse> getMetricDataByTimeSeries(
      PrometheusSetupTestNodeData setupTestNodeData, SettingAttribute settingAttribute, ThirdPartyApiCallLog apiCallLog,
      String hostName) {
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeries = new HashMap<>();
    for (TimeSeries timeSeries : setupTestNodeData.getTimeSeriesToAnalyze()) {
      String url = timeSeries.getUrl();
      Preconditions.checkState(url.contains(START_TIME_PLACE_HOLDER));
      Preconditions.checkState(url.contains(END_TIME_PLACE_HOLDER));
      url = url.replace(START_TIME_PLACE_HOLDER, String.valueOf(setupTestNodeData.getFromTime()));
      url = url.replace(END_TIME_PLACE_HOLDER, String.valueOf(setupTestNodeData.getToTime()));

      if (!setupTestNodeData.isServiceLevel()) {
        Preconditions.checkState(url.contains(HOST_NAME_PLACE_HOLDER));
        url = updateUrlByHostName(url, hostName);
      }
      SyncTaskContext taskContext = SyncTaskContext.builder()
                                        .accountId(settingAttribute.getAccountId())
                                        .appId(GLOBAL_APP_ID)
                                        .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                        .build();
      try {
        PrometheusMetricDataResponse response =
            delegateProxyFactory.get(PrometheusDelegateService.class, taskContext)
                .fetchMetricData((PrometheusConfig) settingAttribute.getValue(), url, apiCallLog);
        metricDataResponseByTimeSeries.put(timeSeries, response);
      } catch (Exception e) {
        logger.info("Exception while trying to collect data for prometheus test: ", e);
        return null;
      }
    }
    return metricDataResponseByTimeSeries;
  }

  /**
   * Returns URL with hostname replaced.
   * @param url
   * @param hostName
   * @return
   */
  private String updateUrlByHostName(String url, String hostName) {
    if (isEmpty(hostName)) {
      return url.substring(0, url.lastIndexOf('{'));
    } else {
      return url.replace(HOST_NAME_PLACE_HOLDER, hostName);
    }
  }
}
