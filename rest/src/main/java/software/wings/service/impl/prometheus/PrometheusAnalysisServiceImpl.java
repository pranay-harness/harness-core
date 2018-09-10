package software.wings.service.impl.prometheus;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
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
public class PrometheusAnalysisServiceImpl implements PrometheusAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusAnalysisServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private MLServiceUtil mlServiceUtil;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(PrometheusSetupTestNodeData setupTestNodeData) {
    final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
    ThirdPartyApiCallLog apiCallLog = apiCallLogWithDummyStateExecution(settingAttribute.getAccountId());

    String hostName = null;
    if (!isEmpty(setupTestNodeData.getInstanceElement().getHostName())) {
      hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    }

    VerificationNodeDataSetupResponse setupResponse =
        VerificationNodeDataSetupResponse.builder().providerReachable(false).build();

    // Request is made without host
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithoutHost =
        getMetricDataByTimeSeries(setupTestNodeData, settingAttribute, apiCallLog, null);

    if (!metricDataResponseByTimeSeriesWithoutHost.isEmpty()) {
      setupResponse = VerificationNodeDataSetupResponse.builder()
                          .providerReachable(true)
                          .loadResponse(VerificationLoadResponse.builder()
                                            .isLoadPresent(metricDataResponseByTimeSeriesWithoutHost.isEmpty())
                                            .loadResponse(metricDataResponseByTimeSeriesWithoutHost)
                                            .build())
                          .build();
    } else {
      // No need to make a call with host if no data is present for without host.
      return setupResponse;
    }

    // Request is made with host
    Map<TimeSeries, PrometheusMetricDataResponse> metricDataResponseByTimeSeriesWithHost =
        getMetricDataByTimeSeries(setupTestNodeData, settingAttribute, apiCallLog, hostName);

    if (!metricDataResponseByTimeSeriesWithHost.isEmpty()) {
      setupResponse = VerificationNodeDataSetupResponse.builder()
                          .providerReachable(true)
                          .loadResponse(VerificationLoadResponse.builder()
                                            .isLoadPresent(metricDataResponseByTimeSeriesWithoutHost.isEmpty())
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
    for (TimeSeries timeSeries : setupTestNodeData.getTimeSeriesToCollect()) {
      String url = timeSeries.getUrl();
      Preconditions.checkState(url.contains(START_TIME_PLACE_HOLDER));
      Preconditions.checkState(url.contains(END_TIME_PLACE_HOLDER));
      Preconditions.checkState(url.contains(HOST_NAME_PLACE_HOLDER));
      url = url.replace(START_TIME_PLACE_HOLDER, String.valueOf(setupTestNodeData.getFromTime()));
      url = url.replace(END_TIME_PLACE_HOLDER, String.valueOf(setupTestNodeData.getToTime()));
      url = updateUrlByHostName(url, hostName);

      SyncTaskContext taskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
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
