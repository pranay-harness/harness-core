package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static software.wings.common.VerificationConstants.WORKFLOW_CV_COLLECTION_CRON_GROUP;
import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.persistence.ReadPref;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.beans.alert.cv.ContinuousVerificationDataCollectionAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
public class DataCollectionCallback implements NotifyCallback {
  @Transient private static final Logger logger = LoggerFactory.getLogger(DataCollectionCallback.class);

  @Inject private transient WaitNotifyEngine waitNotifyEngine;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient AlertService alertService;
  @Inject private transient AppService appService;
  @Inject private transient CVConfigurationService cvConfigurationService;

  private String appId;
  private boolean isLogCollection;
  private StateExecutionData executionData;
  private boolean isSumoDataCollection;
  private String stateExecutionId;
  private StateType stateType;
  private String cvConfigId;
  private long dataCollectionStartTime;
  private long dataCollectionEndTime;

  @Override
  public void notify(Map<String, ResponseData> response) {
    final DataCollectionTaskResult result = (DataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
    if (result.getStatus() == DataCollectionTaskStatus.FAILURE) {
      sendErrorNotification(result.getErrorMessage());
    }

    alertIfNecessary(result.getStatus(), result.getErrorMessage());
  }

  // TODO what is this used for
  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info("notify error for {} ", response.values().iterator().next());
    if (response.values().iterator().next() instanceof ErrorNotifyResponseData) {
      final ErrorNotifyResponseData result = (ErrorNotifyResponseData) response.values().iterator().next();
      sendErrorNotification(result.getErrorMessage());
      alertIfNecessary(DataCollectionTaskStatus.FAILURE, result.getErrorMessage());
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
      deleteDataCollectionCron();
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

  private void alertIfNecessary(DataCollectionTaskStatus status, String errorMessage) {
    if (isEmpty(cvConfigId)) {
      return;
    }
    final CVConfiguration cvConfiguration = cvConfigurationService.getConfiguration(cvConfigId);
    if (cvConfiguration == null) {
      return;
    }
    switch (status) {
      case SUCCESS:
        alertService.closeAlert(appService.getAccountIdByAppId(appId), appId,
            AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT,
            ContinuousVerificationDataCollectionAlert.builder().cvConfiguration(cvConfiguration).build());
        break;
      case FAILURE:
        String message = "Failed to collect data for " + cvConfiguration.getName() + "(Application: "
            + cvConfiguration.getAppName() + ", Environment: " + cvConfiguration.getEnvName() + ") for Time: "
            + new SimpleDateFormat(ContinuousVerificationAlertData.DEFAULT_TIME_FORMAT)
                  .format(new Date(dataCollectionEndTime))
            + "\nReason: " + errorMessage;
        alertService.openAlert(appService.getAccountIdByAppId(appId), appId,
            AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT,
            ContinuousVerificationDataCollectionAlert.builder()
                .cvConfiguration(cvConfiguration)
                .message(message)
                .build());
        break;

      default:
        throw new IllegalStateException("Invalid status " + status);
    }
  }

  private void deleteDataCollectionCron() {
    if (isSumoDataCollection) {
      logger.info("Data collection failed with stateExecutionId {}, deleting data collection cron", stateExecutionId);
      DBCollection collection =
          wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "quartz_verification_jobs");
      BasicDBObject object = new BasicDBObject();
      object.put("keyName", stateExecutionId);
      object.put("keyGroup", stateType.name().toUpperCase() + WORKFLOW_CV_COLLECTION_CRON_GROUP);
      collection.findAndRemove(object);
    }
  }
}
