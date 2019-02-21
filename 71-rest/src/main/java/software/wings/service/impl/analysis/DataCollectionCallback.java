package software.wings.service.impl.analysis;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static software.wings.common.VerificationConstants.WORKFLOW_CV_COLLECTION_CRON_GROUP;
import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.persistence.ReadPref;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
public class DataCollectionCallback implements NotifyCallback {
  @Transient private static final Logger logger = LoggerFactory.getLogger(DataCollectionCallback.class);

  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private WingsPersistence wingsPersistence;

  private String appId;
  private boolean isLogCollection;
  private StateExecutionData executionData;
  private boolean isSumoDataCollection;
  private String stateExecutionId;
  private StateType stateType;

  public DataCollectionCallback(String appId, LogAnalysisExecutionData executionData, boolean isLogCollection,
      boolean isSumoDataCollection, String stateExecutionId, StateType stateType) {
    this.appId = appId;
    this.executionData = executionData;
    this.isLogCollection = isLogCollection;
    this.isSumoDataCollection = isSumoDataCollection;
    this.stateExecutionId = stateExecutionId;
    this.stateType = stateType;
  }

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
