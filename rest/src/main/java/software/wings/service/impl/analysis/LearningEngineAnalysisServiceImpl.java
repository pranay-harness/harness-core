package software.wings.service.impl.analysis;

import static io.harness.persistence.HQuery.excludeAuthority;
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
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

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
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(SERVICE_VERSION_FILE);
      messages.load(in);
    } finally {
      if (in != null) {
        in.close();
      }
    }
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
    LearningEngineAnalysisTask learningEngineAnalysisTask =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter("workflow_execution_id", analysisTask.getWorkflow_execution_id())
            .filter("state_execution_id", analysisTask.getState_execution_id())
            .field("analysis_minute")
            .lessThanOrEq(analysisTask.getAnalysis_minute())
            .filter("version", learningEngineApiVersion)
            .field("executionStatus")
            .in(Lists.newArrayList(ExecutionStatus.RUNNING, ExecutionStatus.QUEUED, ExecutionStatus.SUCCESS))
            .filter("cluster_level", analysisTask.getCluster_level())
            .filter("ml_analysis_type", analysisTask.getMl_analysis_type())
            // TODO can control_nodes be empty ???
            .filter("control_nodes", analysisTask.getControl_nodes())
            .filter("group_name", analysisTask.getGroup_name())
            .order("-createdAt")
            .get();

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

    logger.warn("task is already {}. Will not queue for minute {}, {}", learningEngineAnalysisTask.getExecutionStatus(),
        analysisTask.getAnalysis_minute(), learningEngineAnalysisTask);
    return false;
  }

  @Override
  public boolean addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask analysisTask) {
    LearningEngineExperimentalAnalysisTask experimentalAnalysisTask =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter("state_execution_id", analysisTask.getState_execution_id())
            .get();
    if (experimentalAnalysisTask != null) {
      logger.info("task already queued for experiment {}", analysisTask.getState_execution_id());
      return false;
    }
    analysisTask.setVersion(learningEngineApiVersion);
    analysisTask.setExecutionStatus(ExecutionStatus.QUEUED);
    analysisTask.setRetry(0);
    wingsPersistence.save(analysisTask);
    return true;
  }

  @Override
  public LearningEngineAnalysisTask getNextLearningEngineAnalysisTask(ServiceApiVersion serviceApiVersion) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("version", serviceApiVersion)
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
  public LearningEngineExperimentalAnalysisTask getNextLearningEngineExperimentalAnalysisTask(
      String experimentName, ServiceApiVersion serviceApiVersion) {
    Query<LearningEngineExperimentalAnalysisTask> query =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter("version", serviceApiVersion)
            .filter("experiment_name", experimentName)
            .field("retry")
            .lessThan(LearningEngineAnalysisTask.RETRIES);
    query.or(query.criteria("executionStatus").equal(ExecutionStatus.QUEUED),
        query.and(query.criteria("executionStatus").equal(ExecutionStatus.RUNNING),
            query.criteria("lastUpdatedAt").lessThan(System.currentTimeMillis() - TIME_SERIES_ANALYSIS_TASK_TIME_OUT)));
    UpdateOperations<LearningEngineExperimentalAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineExperimentalAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.RUNNING)
            .inc("retry")
            .set("lastUpdatedAt", System.currentTimeMillis());
    return wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
  }

  @Override
  public boolean hasAnalysisTimedOut(String appId, String workflowExecutionId, String stateExecutionId) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("appId", appId)
                                                  .filter("workflow_execution_id", workflowExecutionId)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("executionStatus", ExecutionStatus.RUNNING)
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
                                                  .filter("workflow_execution_id", workflowExecutionId)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("executionStatus", ExecutionStatus.RUNNING)
                                                  .filter("analysis_minute", analysisMinute)
                                                  .filter("ml_analysis_type", type)
                                                  .filter("cluster_level", level.getLevel());
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.SUCCESS);

    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public Optional<LearningEngineAnalysisTask> earliestQueued() {
    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter("executionStatus", ExecutionStatus.QUEUED)
                                          .order("-createdAt")
                                          .get();

    return task == null ? Optional.empty() : Optional.of(task);
  }

  @Override
  public void cleanup(long keepAfterTimeMillis) {
    Query<LearningEngineAnalysisTask> query =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).field("createdAt").lessThan(keepAfterTimeMillis);
    wingsPersistence.delete(query);
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
  public void markExpTaskCompleted(String taskId) {
    if (taskId == null) {
      logger.warn("taskId is null");
      return;
    }
    wingsPersistence.updateField(
        LearningEngineExperimentalAnalysisTask.class, taskId, "executionStatus", ExecutionStatus.SUCCESS);
  }

  @Override
  public void markStatus(
      String workflowExecutionId, String stateExecutionId, int analysisMinute, ExecutionStatus executionStatus) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("workflow_execution_id", workflowExecutionId)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("executionStatus", ExecutionStatus.RUNNING)
                                                  .filter("cluster_level", getDefaultClusterLevel())
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
    return wingsPersistence.createQuery(ServiceSecretKey.class)
        .filter("serviceType", serviceType)
        .get()
        .getServiceSecret();
  }

  @Override
  public List<MLExperiments> getExperiments(MLAnalysisType ml_analysis_type) {
    return wingsPersistence.createQuery(MLExperiments.class, excludeAuthority)
        .filter("ml_analysis_type", ml_analysis_type)
        .asList();
  }
}
