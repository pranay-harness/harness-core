package software.wings.service.impl.prometheus;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.MLServiceUtil;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by Pranjal on 09/02/2018
 */
@ValidateOnExecution
@Singleton
public class PrometheusAnalysisServiceImpl implements PrometheusAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusAnalysisServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private MLServiceUtil mlServiceUtil;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(PrometheusSetupTestNodeData setupTestNodeData) {
    final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
    ThirdPartyApiCallLog apiCallLog =
        createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getAppId(), setupTestNodeData.getGuid());

    String hostName = null;
    if (!setupTestNodeData.isServiceLevel()) {
      hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    }

    VerificationNodeDataSetupResponse setupResponse =
        VerificationNodeDataSetupResponse.builder().providerReachable(false).build();

    // Request is made without host
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithoutHost =
        getMetricDataByTimeSeries(setupTestNodeData, settingAttribute, apiCallLog, null);

    if (setupTestNodeData.isServiceLevel() || !metricDataResponseByTimeSeriesWithoutHost.isEmpty()) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder()
                            .isLoadPresent(!metricDataResponseByTimeSeriesWithoutHost.isEmpty())
                            .loadResponse(metricDataResponseByTimeSeriesWithoutHost)
                            .build())
          .build();
    }

    // No need to make a call with host if no data is present for without host.
    if (metricDataResponseByTimeSeriesWithoutHost.isEmpty()) {
      return setupResponse;
    }

    // Request is made with host
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithHost =
        getMetricDataByTimeSeries(setupTestNodeData, settingAttribute, apiCallLog, hostName);

    if (!metricDataResponseByTimeSeriesWithHost.isEmpty()) {
      setupResponse = VerificationNodeDataSetupResponse.builder()
                          .providerReachable(true)
                          .loadResponse(VerificationLoadResponse.builder()
                                            .isLoadPresent(!metricDataResponseByTimeSeriesWithoutHost.isEmpty())
                                            .loadResponse(metricDataResponseByTimeSeriesWithHost)
                                            .build())
                          .dataForNode(metricDataResponseByTimeSeriesWithHost)
                          .build();
    }
    return setupResponse;
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
      } catch (IOException e) {
        return metricDataResponseByTimeSeries;
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
