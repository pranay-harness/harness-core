package software.wings.service.impl.analysis;

import static software.wings.delegatetasks.NewRelicDataCollectionTask.COLLECTION_PERIOD_MINS;
import static software.wings.service.impl.newrelic.LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT;
import static software.wings.utils.Misc.generateSecretKey;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/9/18.
 */
public class LearningEngineAnalysisServiceImpl implements LearningEngineService {
  private static final Logger logger = LoggerFactory.getLogger(LearningEngineAnalysisServiceImpl.class);

  private static final String SERVICE_VERSION_FILE = "/service_version.properties";

  @Inject private WingsPersistence wingsPersistence;
  private final ServiceApiVersion learningEngineApiVersion;

  public LearningEngineAnalysisServiceImpl() throws IOException {
    Properties messages = new Properties();
    InputStream in = getClass().getResourceAsStream(SERVICE_VERSION_FILE);
    messages.load(in);
    String apiVersion = messages.getProperty(ServiceType.LEARNING_ENGINE.name());
    Preconditions.checkState(!StringUtils.isEmpty(apiVersion));
    learningEngineApiVersion = ServiceApiVersion.valueOf(apiVersion.toUpperCase());
  }

  private ClusterLevel getDefaultClusterLevel() {
    return ClusterLevel.HF;
  }

  @Override
  public boolean addLearningEngineAnalysisTask(LearningEngineAnalysisTask analysisTask) {
    analysisTask.setVersion(learningEngineApiVersion);
    analysisTask.setExecutionStatus(ExecutionStatus.QUEUED);
    analysisTask.setRetry(0);
    if (analysisTask.getCluster_level() == null) {
      analysisTask.setCluster_level(getDefaultClusterLevel().getLevel());
    }
    Query<LearningEngineAnalysisTask> query =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .field("workflow_execution_id")
            .equal(analysisTask.getWorkflow_execution_id())
            .field("state_execution_id")
            .equal(analysisTask.getState_execution_id())
            .field("analysis_minute")
            .lessThanOrEq(analysisTask.getAnalysis_minute())
            .field("version")
            .equal(learningEngineApiVersion)
            .field("executionStatus")
            .in(Lists.newArrayList(ExecutionStatus.RUNNING, ExecutionStatus.QUEUED, ExecutionStatus.SUCCESS))
            .field("cluster_level")
            .equal(analysisTask.getCluster_level())
            .field("ml_analysis_type")
            .equal(analysisTask.getMl_analysis_type())
            // TODO can control_nodes be empty ???
            .field("control_nodes")
            .equal(analysisTask.getControl_nodes())
            .order("-createdAt");
    LearningEngineAnalysisTask learningEngineAnalysisTask = wingsPersistence.executeGetOneQuery(query);

    if (learningEngineAnalysisTask == null) {
      wingsPersistence.save(analysisTask);
      return true;
    }

    if (learningEngineAnalysisTask.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      if (learningEngineAnalysisTask.getAnalysis_minute() < analysisTask.getAnalysis_minute()) {
        wingsPersistence.save(analysisTask);
        return true;
      }
      logger.warn("task is already marked success for min {}. task {}", analysisTask.getAnalysis_minute(),
          learningEngineAnalysisTask);
      return false;
    }

    // task has been sitting for a while without being executed
    if (analysisTask.getAnalysis_minute() - learningEngineAnalysisTask.getAnalysis_minute()
        >= COLLECTION_PERIOD_MINS + TimeUnit.MILLISECONDS.toMinutes(TIME_SERIES_ANALYSIS_TASK_TIME_OUT)) {
      throw new WingsException(
          ErrorCode.NEWRELIC_ERROR, "Analysis timed out for minute " + learningEngineAnalysisTask.getAnalysis_minute());
    }
    logger.warn("task is already {}. Will not queue for minute {}, {}", learningEngineAnalysisTask.getExecutionStatus(),
        analysisTask.getAnalysis_minute(), learningEngineAnalysisTask);
    return false;
  }

  @Override
  public LearningEngineAnalysisTask getNextLearningEngineAnalysisTask(ServiceApiVersion serviceApiVersion) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .field("version")
                                                  .equal(serviceApiVersion)
                                                  .field("retry")
                                                  .lessThan(LearningEngineAnalysisTask.RETRIES);
    query.or(query.criteria("executionStatus").equal(ExecutionStatus.QUEUED),
        query.and(query.criteria("executionStatus").equal(ExecutionStatus.RUNNING),
            query.criteria("lastUpdatedAt").lessThan(System.currentTimeMillis() - TIME_SERIES_ANALYSIS_TASK_TIME_OUT)));
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.RUNNING)
            .inc("retry")
            .set("lastUpdatedAt", System.currentTimeMillis());
    return wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
  }

  @Override
  public boolean hasAnalysisTimedOut(String workflowExecutionId, String stateExecutionId) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .field("workflow_execution_id")
                                                  .equal(workflowExecutionId)
                                                  .field("state_execution_id")
                                                  .equal(stateExecutionId)
                                                  .field("executionStatus")
                                                  .equal(ExecutionStatus.RUNNING)
                                                  .field("retry")
                                                  .greaterThanOrEq(LearningEngineAnalysisTask.RETRIES);
    return !query.asList().isEmpty();
  }

  @Override
  public void markCompleted(
      String workflowExecutionId, String stateExecutionId, int analysisMinute, MLAnalysisType type) {
    markCompleted(workflowExecutionId, stateExecutionId, analysisMinute, type, getDefaultClusterLevel());
  }

  @Override
  public void markCompleted(String workflowExecutionId, String stateExecutionId, int analysisMinute,
      MLAnalysisType type, ClusterLevel level) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .field("workflow_execution_id")
                                                  .equal(workflowExecutionId)
                                                  .field("state_execution_id")
                                                  .equal(stateExecutionId)
                                                  .field("executionStatus")
                                                  .equal(ExecutionStatus.RUNNING)
                                                  .field("analysis_minute")
                                                  .equal(analysisMinute)
                                                  .field("ml_analysis_type")
                                                  .equal(type)
                                                  .field("cluster_level")
                                                  .equal(level.getLevel());
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.SUCCESS);

    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public void markCompleted(String taskId) {
    if (taskId == null) {
      logger.warn("taskId is null");
      return;
    }
    wingsPersistence.updateField(LearningEngineAnalysisTask.class, taskId, "executionStatus", ExecutionStatus.SUCCESS);
  }

  @Override
  public void markStatus(
      String workflowExecutionId, String stateExecutionId, int analysisMinute, ExecutionStatus executionStatus) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .field("workflow_execution_id")
                                                  .equal(workflowExecutionId)
                                                  .field("state_execution_id")
                                                  .equal(stateExecutionId)
                                                  .field("executionStatus")
                                                  .equal(ExecutionStatus.RUNNING)
                                                  .field("cluster_level")
                                                  .equal(getDefaultClusterLevel())
                                                  .field("analysis_minute")
                                                  .lessThanOrEq(analysisMinute);
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", executionStatus);

    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public void initializeServiceSecretKeys() {
    for (ServiceType serviceType : ServiceType.values()) {
      wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(
          ServiceSecretKey.builder().serviceType(serviceType).serviceSecret(generateSecretKey()).build()));
    }
  }

  @Override
  public String getServiceSecretKey(ServiceType serviceType) {
    Preconditions.checkNotNull(serviceType);
    Query<ServiceSecretKey> query =
        wingsPersistence.createQuery(ServiceSecretKey.class).field("serviceType").equal(serviceType);
    return wingsPersistence.executeGetOneQuery(query).getServiceSecret();
  }
}
