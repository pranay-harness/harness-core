package software.wings.service.impl.analysis;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.task.protocol.ResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
public class DataCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(DataCollectionCallback.class);

  @Inject private WaitNotifyEngine waitNotifyEngine;

  private String appId;
  private boolean isLogCollection;
  private StateExecutionData executionData;

  public DataCollectionCallback() {}

  public DataCollectionCallback(String appId, StateExecutionData executionData, boolean isLogCollection) {
    this.appId = appId;
    this.executionData = executionData;
    this.isLogCollection = isLogCollection;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    final DataCollectionTaskResult result = (DataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
    if (result.getStatus() == DataCollectionTaskStatus.FAILURE) {
      sendErrorNotification(result.getErrorMessage());
    }
  }

  // TODO what is this used for
  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info("notify error for {} ", response.values().iterator().next());
    if (response.values().iterator().next() instanceof ErrorNotifyResponseData) {
      final ErrorNotifyResponseData result = (ErrorNotifyResponseData) response.values().iterator().next();
      sendErrorNotification(result.getErrorMessage());
    }
  }
  private void sendErrorNotification(String errorMsg) {
    Preconditions.checkNotNull(executionData, "execution data is null for appId" + appId);
    if (isLogCollection) {
      final LogAnalysisExecutionData logAnalysisExecutionData = (LogAnalysisExecutionData) executionData;
      logAnalysisExecutionData.setStatus(ExecutionStatus.ERROR);
      logAnalysisExecutionData.setErrorMsg(errorMsg);
      waitNotifyEngine.notify(logAnalysisExecutionData.getCorrelationId(),
          aLogAnalysisResponse()
              .withLogAnalysisExecutionData(logAnalysisExecutionData)
              .withExecutionStatus(ExecutionStatus.ERROR)
              .build());
    } else {
      MetricAnalysisExecutionData analysisExecutionData = (MetricAnalysisExecutionData) executionData;
      analysisExecutionData.setStatus(ExecutionStatus.ERROR);
      analysisExecutionData.setErrorMsg(errorMsg);
      MetricDataAnalysisResponse metricDataAnalysisResponse =
          MetricDataAnalysisResponse.builder().stateExecutionData(analysisExecutionData).build();
      metricDataAnalysisResponse.setExecutionStatus(ExecutionStatus.ERROR);
      waitNotifyEngine.notify(analysisExecutionData.getCorrelationId(), metricDataAnalysisResponse);
    }
  }
}
