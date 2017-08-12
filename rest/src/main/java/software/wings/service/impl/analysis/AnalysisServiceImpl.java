package software.wings.service.impl.analysis;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.common.base.Preconditions;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.SplunkConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AnalysisServiceImpl implements AnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);
  private final Random random = new Random();

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected WorkflowExecutionService workflowExecutionService;

  @Override
  public Boolean saveLogData(StateType stateType, String appId, String stateExecutionId, String workflowId,
      String workflowExecutionId, List<LogElement> logData) throws IOException {
    logger.debug("inserting " + logData.size() + " pieces of splunk log data");
    final List<LogDataRecord> logDataRecords =
        LogDataRecord.generateDataRecords(stateType, appId, stateExecutionId, workflowId, workflowExecutionId, logData);
    wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
    logger.debug("inserted " + logDataRecords.size() + " LogDataRecord to persistence layer.");
    return true;
  }

  @Override
  public List<LogDataRecord> getLogData(LogRequest logRequest, boolean compareCurrent, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecordQuery = null;
    List<LogDataRecord> records = null;
    if (compareCurrent) {
      splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(stateType)
                                     .field("stateExecutionId")
                                     .equal(logRequest.getStateExecutionId())
                                     .field("applicationId")
                                     .equal(logRequest.getApplicationId())
                                     .field("query")
                                     .equal(logRequest.getQuery())
                                     .field("processed")
                                     .equal(false)
                                     .field("logCollectionMinute")
                                     .equal(logRequest.getLogCollectionMinute())
                                     .field("host")
                                     .hasAnyOf(logRequest.getNodes());
    } else {
      final WorkflowExecution workflowExecution =
          getLastSuccessfulWorkflowExecution(logRequest.getApplicationId(), logRequest.getWorkflowId());
      Preconditions.checkNotNull(
          workflowExecution, "No successful workflow execution found for workflowId: " + logRequest.getWorkflowId());

      final PageRequest<LogDataRecord> lastSuccessfulExecutionData =
          PageRequest.Builder.aPageRequest()
              .addFilter("stateType", Operator.EQ, stateType)
              .addFilter("workflowId", Operator.EQ, logRequest.getWorkflowId())
              .addFilter("workflowExecutionId", Operator.EQ, workflowExecution.getUuid())
              .addFilter("stateExecutionId", Operator.NOT_EQ, logRequest.getStateExecutionId())
              .addFilter("applicationId", Operator.EQ, logRequest.getApplicationId())
              .addFilter("query", Operator.EQ, logRequest.getQuery())
              .addFilter("logCollectionMinute", Operator.EQ, logRequest.getLogCollectionMinute())
              .addOrder("createdAt", OrderType.DESC)
              .withLimit("1")
              .build();

      PageResponse<LogDataRecord> lastSuccessfullRecords =
          wingsPersistence.query(LogDataRecord.class, lastSuccessfulExecutionData);

      Preconditions.checkState(lastSuccessfullRecords.size() == 1, "Multiple records found for give query");

      LogDataRecord record = lastSuccessfullRecords.get(0);
      if (record == null) {
        logger.error("Could not find any logs collected for minute {} for previous successful workflow {}",
            logRequest.getLogCollectionMinute(), logRequest.getWorkflowId());
        return Collections.emptyList();
      }
      logger.info("returning logs for workflowExecutionId: " + workflowExecution.getWorkflowId()
          + " stateExecutionId: " + record.getStateExecutionId());
      splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(stateType)
                                     .field("stateExecutionId")
                                     .equal(record.getStateExecutionId())
                                     .field("applicationId")
                                     .equal(logRequest.getApplicationId())
                                     .field("query")
                                     .equal(logRequest.getQuery())
                                     .field("host")
                                     .hasAnyOf(logRequest.getNodes())
                                     .field("logCollectionMinute")
                                     .equal(logRequest.getLogCollectionMinute());
    }

    records = splunkLogDataRecordQuery.asList();
    logger.debug("returning " + records.size() + " records for request: " + logRequest);
    return records;
  }

  @Override
  public Boolean markProcessed(String stateExecutionId, String applicationId, long tillTimeStamp, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecords = wingsPersistence.createQuery(LogDataRecord.class)
                                                    .field("stateType")
                                                    .equal(stateType)
                                                    .field("stateExecutionId")
                                                    .equal(stateExecutionId)
                                                    .field("applicationId")
                                                    .equal("applicationId")
                                                    .field("timeStamp")
                                                    .lessThanOrEq(tillTimeStamp);

    wingsPersistence.update(
        splunkLogDataRecords, wingsPersistence.createUpdateOperations(LogDataRecord.class).set("processed", true));
    return true;
  }

  @Override
  public boolean isLogDataCollected(
      String applicationId, String stateExecutionId, String query, int logCollectionMinute, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                                        .field("stateType")
                                                        .equal(stateType)
                                                        .field("stateExecutionId")
                                                        .equal(stateExecutionId)
                                                        .field("applicationId")
                                                        .equal(applicationId)
                                                        .field("query")
                                                        .equal(query)
                                                        .field("logCollectionMinute")
                                                        .equal(logCollectionMinute);
    return splunkLogDataRecordQuery.asList().size() > 0;
  }

  private WorkflowExecution getLastSuccessfulWorkflowExecution(String appId, String workflowId) {
    final PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
                                                           .addOrder("createdAt", OrderType.DESC)
                                                           .withLimit("1")
                                                           .build();

    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, false);
    if (workflowExecutions.isEmpty()) {
      logger.error("Could not get a successful workflow to find control nodes");
      return null;
    }

    Preconditions.checkState(workflowExecutions.size() == 1, "Multiple workflows found for give query");
    return workflowExecutions.get(0);
  }

  @Override
  public Boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType) {
    wingsPersistence.delete(wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                .field("applicationId")
                                .equal(mlAnalysisResponse.getApplicationId())
                                .field("stateExecutionId")
                                .equal(mlAnalysisResponse.getStateExecutionId())
                                .field("stateType")
                                .equal(stateType));

    mlAnalysisResponse.setStateType(stateType);
    wingsPersistence.save(mlAnalysisResponse);
    logger.debug(
        "inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getApplicationId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    return true;
  }

  @Override
  public LogMLAnalysisRecord getLogAnalysisRecords(
      String applicationId, String stateExecutionId, String query, StateType stateType) {
    Query<LogMLAnalysisRecord> splunkLogMLAnalysisRecords = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                                .field("stateExecutionId")
                                                                .equal(stateExecutionId)
                                                                .field("applicationId")
                                                                .equal(applicationId)
                                                                .field("query")
                                                                .equal(query)
                                                                .field("stateType")
                                                                .equal(stateType);
    return wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
  }

  @Override
  public LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId, StateType stateType) {
    Query<LogMLAnalysisRecord> splunkLogMLAnalysisRecords = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                                .field("stateExecutionId")
                                                                .equal(stateExecutionId)
                                                                .field("applicationId")
                                                                .equal(applicationId)
                                                                .field("stateType")
                                                                .equal(stateType);
    LogMLAnalysisRecord analysisRecord = wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
    if (analysisRecord == null) {
      return null;
    }
    final LogMLAnalysisSummary analysisSummary = new LogMLAnalysisSummary();
    analysisSummary.setQuery(analysisRecord.getQuery());
    analysisSummary.setControlClusters(computeCluster(analysisRecord.getControl_clusters()));
    analysisSummary.setTestClusters(computeCluster(analysisRecord.getTest_clusters()));
    analysisSummary.setUnknownClusters(computeCluster(analysisRecord.getUnknown_clusters()));

    RiskLevel riskLevel = RiskLevel.LOW;
    String analysisSummaryMsg =
        analysisRecord.getAnalysisSummaryMessage() == null || analysisRecord.getAnalysisSummaryMessage().isEmpty()
        ? "No anomaly found"
        : analysisRecord.getAnalysisSummaryMessage();

    int unknownClusters = 0;
    if (analysisSummary.getUnknownClusters() != null && analysisSummary.getUnknownClusters().size() > 0) {
      riskLevel = RiskLevel.HIGH;
      unknownClusters = analysisSummary.getUnknownClusters().size();
    }

    int unknownFrequency = getUnexpectedFrequency(analysisSummary);
    if (unknownFrequency > 0) {
      riskLevel = RiskLevel.HIGH;
    }

    if (unknownClusters > 0 || unknownFrequency > 0) {
      final int totalAnomalies = unknownClusters + unknownFrequency;
      analysisSummaryMsg =
          totalAnomalies == 1 ? totalAnomalies + " anomalous event found" : totalAnomalies + " anomalous events found";
    }

    analysisSummary.setRiskLevel(riskLevel);
    analysisSummary.setAnalysisSummaryMessage(analysisSummaryMsg);
    return analysisSummary;
  }

  @Override
  public void validateConfig(final SettingAttribute settingAttribute, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      switch (stateType) {
        case SPLUNKV2:
          errorCode = ErrorCode.SPLUNK_CONFIGURATION_ERROR;
          SyncTaskContext syncTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(SplunkDelegateService.class, syncTaskContext)
              .validateConfig((SplunkConfig) settingAttribute.getValue());
          break;
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .validateConfig((ElkConfig) settingAttribute.getValue());
          break;
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, "reason", e.getMessage());
    }
  }

  private List<LogMLClusterSummary> computeCluster(Map<String, Map<String, SplunkAnalysisCluster>> cluster) {
    if (cluster == null) {
      return Collections.emptyList();
    }
    final List<LogMLClusterSummary> analysisSummaries = new ArrayList<>();
    for (Entry<String, Map<String, SplunkAnalysisCluster>> labelEntry : cluster.entrySet()) {
      final LogMLClusterSummary clusterSummary = new LogMLClusterSummary();
      clusterSummary.setHostSummary(new HashMap<>());
      for (Entry<String, SplunkAnalysisCluster> hostEntry : labelEntry.getValue().entrySet()) {
        final LogMLHostSummary hostSummary = new LogMLHostSummary();
        final SplunkAnalysisCluster analysisCluster = hostEntry.getValue();
        hostSummary.setXCordinate(sprinkalizedCordinate(analysisCluster.getX()));
        hostSummary.setYCordinate(sprinkalizedCordinate(analysisCluster.getY()));
        hostSummary.setUnexpectedFreq((analysisCluster.isUnexpected_freq()));
        hostSummary.setCount(computeCountFromFrequencies(analysisCluster));
        clusterSummary.setLogText(analysisCluster.getText());
        clusterSummary.setTags(analysisCluster.getTags());
        clusterSummary.getHostSummary().put(hostEntry.getKey(), hostSummary);
        analysisSummaries.add(clusterSummary);
      }
    }

    return analysisSummaries;
  }

  private int computeCountFromFrequencies(SplunkAnalysisCluster analysisCluster) {
    int count = 0;
    for (Map frequency : analysisCluster.getMessage_frequencies()) {
      if (!frequency.containsKey("count")) {
        continue;
      }

      count += (Integer) frequency.get("count");
    }

    return count;
  }

  private int getUnexpectedFrequency(LogMLAnalysisSummary analysisSummary) {
    int unexpectedFrequency = 0;
    if (analysisSummary.getTestClusters() == null) {
      return unexpectedFrequency;
    }

    for (LogMLClusterSummary clusterSummary : analysisSummary.getTestClusters()) {
      for (Entry<String, LogMLHostSummary> hostEntry : clusterSummary.getHostSummary().entrySet()) {
        if (hostEntry.getValue().isUnexpectedFreq()) {
          unexpectedFrequency++;
        }
      }
    }

    return unexpectedFrequency;
  }

  private double sprinkalizedCordinate(double coordinate) {
    final int sprinkleRatio = random.nextInt() % 8;
    double adjustmentBase = coordinate - Math.floor(coordinate);
    return coordinate + (adjustmentBase * sprinkleRatio) / 100;
  }
}
