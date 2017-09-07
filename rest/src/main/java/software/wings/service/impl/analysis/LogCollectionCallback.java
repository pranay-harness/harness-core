package software.wings.service.impl.analysis;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;
import static software.wings.sm.ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.ABORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult.LogDataCollectionTaskStatus;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */

public class LogCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(LogCollectionCallback.class);

  //  @Inject
  //  private ExecutionInterruptManager executionInterruptManager;
  @com.google.inject.Inject private WaitNotifyEngine waitNotifyEngine;

  private String appId;
  private String correlationId;

  public LogCollectionCallback() {}

  public LogCollectionCallback(String appId, String correlationId) {
    this.appId = appId;
    this.correlationId = correlationId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    final LogDataCollectionTaskResult result = (LogDataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
    if (result.getStatus() == LogDataCollectionTaskStatus.FAILURE) {
      final LogAnalysisExecutionData executionData = LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
                                                         .withStatus(ExecutionStatus.FAILED)
                                                         .withErrorMsg(result.getErrorMessage())
                                                         .build();
      waitNotifyEngine.notify(correlationId,
          aLogAnalysisResponse()
              .withLogAnalysisExecutionData(executionData)
              .withExecutionStatus(ExecutionStatus.FAILED)
              .build());
    }
  }

  // TODO what is this used for
  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.info("error in data collection for app " + appId);
  }
}
