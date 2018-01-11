package software.wings.service.impl.analysis;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData.StateExecutionDataBuilder;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
public class DataCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(DataCollectionCallback.class);

  @Inject private WaitNotifyEngine waitNotifyEngine;

  private String appId;
  private String correlationId;
  private boolean isLogCollection;

  public DataCollectionCallback() {}

  public DataCollectionCallback(String appId, String correlationId, boolean isLogCollection) {
    this.appId = appId;
    this.correlationId = correlationId;
    this.isLogCollection = isLogCollection;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    final DataCollectionTaskResult result = (DataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
    if (result.getStatus() == DataCollectionTaskStatus.FAILURE) {
      if (isLogCollection) {
        final LogAnalysisExecutionData executionData = LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
                                                           .withStatus(ExecutionStatus.FAILED)
                                                           .withErrorMsg(result.getErrorMessage())
                                                           .build();
        waitNotifyEngine.notify(correlationId,
            aLogAnalysisResponse()
                .withLogAnalysisExecutionData(executionData)
                .withExecutionStatus(ExecutionStatus.FAILED)
                .build());
      } else {
        MetricDataAnalysisResponse metricDataAnalysisResponse =
            MetricDataAnalysisResponse.builder()
                .stateExecutionData(StateExecutionDataBuilder.aStateExecutionData()
                                        .withStatus(ExecutionStatus.FAILED)
                                        .withErrorMsg(result.getErrorMessage())
                                        .build())
                .build();
        metricDataAnalysisResponse.setExecutionStatus(ExecutionStatus.FAILED);
        waitNotifyEngine.notify(correlationId, metricDataAnalysisResponse);
      }
    }
  }

  // TODO what is this used for
  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.info("error in data collection for app " + appId);
  }
}
