package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by rsingh on 9/26/17.
 */
public class MetricDataAnalysisServiceImpl implements MetricDataAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(MetricDataAnalysisServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private LearningEngineService learningEngineService;
  @Inject protected DelegateServiceImpl delegateService;

  @Override
  public boolean saveMetricData(String accountId, String applicationId, String stateExecutionId, String delegateTaskId,
      List<NewRelicMetricDataRecord> metricData) throws IOException {
    if (!isStateValid(applicationId, stateExecutionId)) {
      logger.warn("State is no longer active " + metricData.get(0).getStateExecutionId()
          + ". Sending delegate abort request " + delegateTaskId);
      delegateService.abortTask(accountId, delegateTaskId);
      return false;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("inserting " + metricData.size() + " pieces of new relic metrics data");
    }
    wingsPersistence.saveIgnoringDuplicateKeys(metricData);
    if (logger.isDebugEnabled()) {
      logger.debug("inserted " + metricData.size() + " NewRelicMetricDataRecord to persistence layer.");
    }
    return true;
  }

  @Override
  public boolean saveMetricNames(String accountId, NewRelicMetricNames metricNames) throws IOException {
    wingsPersistence.save(metricNames);
    return true;
  }

  @Override
  public boolean addMetricNamesWorkflowInfo(String accountId, NewRelicMetricNames metricNames) throws IOException {
    Query<NewRelicMetricNames> query = wingsPersistence.createQuery(NewRelicMetricNames.class)
                                           .field("newRelicAppId")
                                           .equal(metricNames.getNewRelicAppId())
                                           .field("newRelicConfigId")
                                           .equal(metricNames.getNewRelicConfigId());
    wingsPersistence.update(query,
        wingsPersistence.createUpdateOperations(NewRelicMetricNames.class)
            .addToSet("registeredWorkflows", metricNames.getRegisteredWorkflows()));
    return true;
  }

  @Override
  public boolean updateMetricNames(String accountId, NewRelicMetricNames metricNames) throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("updating " + metricNames.getMetrics().size() + " pieces of new relic metric names for {}, {}",
          metricNames.getNewRelicAppId(), metricNames.getNewRelicConfigId());
    }
    Query<NewRelicMetricNames> query = wingsPersistence.createQuery(NewRelicMetricNames.class)
                                           .field("newRelicAppId")
                                           .equal(metricNames.getNewRelicAppId())
                                           .field("newRelicConfigId")
                                           .equal(metricNames.getNewRelicConfigId());
    wingsPersistence.update(query,
        wingsPersistence.createUpdateOperations(NewRelicMetricNames.class)
            .set("metrics", metricNames.getMetrics())
            .set("lastUpdatedTime", System.currentTimeMillis()));
    return true;
  }

  @Override
  public NewRelicMetricNames getMetricNames(String newRelicAppId, String newRelicServerConfigId) throws IOException {
    return wingsPersistence.createQuery(NewRelicMetricNames.class)
        .field("newRelicAppId")
        .equal(newRelicAppId)
        .field("newRelicConfigId")
        .equal(newRelicServerConfigId)
        .get();
  }

  @Override
  public List<NewRelicMetricNames> listMetricNamesWithWorkflows() {
    return wingsPersistence.createQuery(NewRelicMetricNames.class)
        .field("registeredWorkflows")
        .exists()
        .project("metrics", false)
        .asList();
  }

  @Override
  public boolean saveAnalysisRecords(NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                .field("workflowExecutionId")
                                .equal(metricAnalysisRecord.getWorkflowExecutionId())
                                .field("stateExecutionId")
                                .equal(metricAnalysisRecord.getStateExecutionId()));

    wingsPersistence.save(metricAnalysisRecord);
    if (logger.isDebugEnabled()) {
      logger.debug("inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: "
          + metricAnalysisRecord.getWorkflowExecutionId()
          + " StateExecutionInstanceId: " + metricAnalysisRecord.getStateExecutionId());
    }
    return true;
  }

  @Override
  public boolean saveAnalysisRecordsML(StateType stateType, String accountId, String applicationId,
      String stateExecutionId, final String workflowExecutionId, final String workflowId, String serviceId,
      Integer analysisMinute, String taskId, TimeSeriesMLAnalysisRecord mlAnalysisResponse) {
    mlAnalysisResponse.setStateType(stateType);
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setAnalysisMinute(analysisMinute);

    TimeSeriesMLScores timeSeriesMLScores = TimeSeriesMLScores.builder()
                                                .applicationId(applicationId)
                                                .stateExecutionId(stateExecutionId)
                                                .workflowExecutionId(workflowExecutionId)
                                                .workflowId(workflowId)
                                                .analysisMinute(analysisMinute)
                                                .stateType(stateType)
                                                .scoresMap(new HashMap<>())
                                                .build();

    int txnId = 0;
    int metricId;
    for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
      TimeSeriesMLTxnScores txnScores =
          TimeSeriesMLTxnScores.builder().transactionName(txnSummary.getTxn_name()).scoresMap(new HashMap<>()).build();
      timeSeriesMLScores.getScoresMap().put(String.valueOf(txnId), txnScores);

      metricId = 0;
      for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
        if (mlMetricSummary.getResults() != null) {
          TimeSeriesMLMetricScores mlMetricScores = TimeSeriesMLMetricScores.builder()
                                                        .metricName(mlMetricSummary.getMetric_name())
                                                        .scores(new ArrayList<>())
                                                        .build();
          txnScores.getScoresMap().put(String.valueOf(metricId), mlMetricScores);

          Iterator<Entry<String, TimeSeriesMLHostSummary>> it = mlMetricSummary.getResults().entrySet().iterator();
          Map<String, TimeSeriesMLHostSummary> timeSeriesMLHostSummaryMap = new HashMap<>();
          while (it.hasNext()) {
            Entry<String, TimeSeriesMLHostSummary> pair = it.next();
            timeSeriesMLHostSummaryMap.put(pair.getKey().replaceAll("\\.", "-"), pair.getValue());
            mlMetricScores.getScores().add(pair.getValue().getScore());
          }
          mlMetricSummary.setResults(timeSeriesMLHostSummaryMap);
          ++metricId;
        }
      }
      ++txnId;
    }

    saveTimeSeriesMLScores(timeSeriesMLScores);
    bumpCollectionMinuteToProcess(stateType, stateExecutionId, workflowExecutionId, serviceId, analysisMinute);
    learningEngineService.markCompleted(taskId);

    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .field("workflowExecutionId")
                                .equal(workflowExecutionId)
                                .field("stateExecutionId")
                                .equal(stateExecutionId));

    wingsPersistence.save(mlAnalysisResponse);
    if (logger.isDebugEnabled()) {
      logger.debug("inserted NewRelicMetricAnalysisRecord to persistence layer for "
              + "stateType: {}, workflowExecutionId: {} StateExecutionInstanceId: {}",
          stateType, workflowExecutionId, stateExecutionId);
    }
    return true;
  }

  @Override
  public List<TimeSeriesMLScores> getTimeSeriesMLScores(
      String applicationId, String workflowId, int analysisMinute, int limit) {
    List<String> workflowExecutionIds = getLastSuccessfulWorkflowExecutionIds(workflowId);
    return wingsPersistence.createQuery(TimeSeriesMLScores.class)
        .field("workflowId")
        .equal(workflowId)
        .field("applicationId")
        .equal(applicationId)
        .field("analysisMinute")
        .equal(analysisMinute)
        .field("workflowExecutionIds")
        .in(workflowExecutionIds)
        .order("-createdAt")
        .asList(new FindOptions().limit(limit));
  }

  @Override
  public void saveTimeSeriesMLScores(TimeSeriesMLScores scores) {
    wingsPersistence.save(scores);
  }

  @Override
  public List<NewRelicMetricDataRecord> getRecords(StateType stateType, String workflowExecutionId,
      String stateExecutionId, String workflowId, String serviceId, Set<String> nodes, int analysisMinute,
      int analysisStartMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowId")
                                                .equal(workflowId)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionId)
                                                .field("stateExecutionId")
                                                .equal(stateExecutionId)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("host")
                                                .hasAnyOf(nodes)
                                                .field("level")
                                                .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute)
                                                .field("dataCollectionMinute")
                                                .greaterThanOrEq(analysisStartMinute);
    return query.asList();
  }

  @Override
  public List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(StateType stateType, String workflowId,
      String workflowExecutionID, String serviceId, int analysisMinute, int analysisStartMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowId")
                                                .equal(workflowId)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionID)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute)
                                                .field("dataCollectionMinute")
                                                .greaterThanOrEq(analysisStartMinute);
    return query.asList();
  }

  @Override
  public List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      StateType stateType, String workflowId, String serviceId, int analysisMinute, int analysisStartMinute) {
    final String astSuccessfulWorkflowExecutionIdWithData =
        getLastSuccessfulWorkflowExecutionIdWithData(stateType, workflowId, serviceId);
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowId")
                                                .equal(workflowId)
                                                .field("workflowExecutionId")
                                                .equal(astSuccessfulWorkflowExecutionIdWithData)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute)
                                                .field("dataCollectionMinute")
                                                .greaterThanOrEq(analysisStartMinute);
    return query.asList();
  }

  @Override
  public int getMaxControlMinuteWithData(
      StateType stateType, String serviceId, String workflowId, String workflowExecutionId) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .field("stateType")
                                                            .equal(stateType)
                                                            .field("workflowId")
                                                            .equal(workflowId)
                                                            .field("workflowExecutionId")
                                                            .equal(workflowExecutionId)
                                                            .field("serviceId")
                                                            .equal(serviceId)
                                                            .field("level")
                                                            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                            .order("-dataCollectionMinute")
                                                            .get(new FindOptions().limit(1));

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public int getMinControlMinuteWithData(
      StateType stateType, String serviceId, String workflowId, String workflowExecutionId) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .field("stateType")
                                                            .equal(stateType)
                                                            .field("workflowId")
                                                            .equal(workflowId)
                                                            .field("workflowExecutionId")
                                                            .equal(workflowExecutionId)
                                                            .field("serviceId")
                                                            .equal(serviceId)
                                                            .field("level")
                                                            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                            .order("dataCollectionMinute")
                                                            .get(new FindOptions().limit(1));

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithData(StateType stateType, String workflowId, String serviceId) {
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(workflowId);
    for (String successfulExecution : successfulExecutions) {
      List<NewRelicMetricDataRecord> lastSuccessfulRecords =
          wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
              .field("stateType")
              .equal(stateType)
              .field("workflowId")
              .equal(workflowId)
              .field("workflowExecutionId")
              .equal(successfulExecution)
              .field("serviceId")
              .equal(serviceId)
              .field("level")
              .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
              .asList(new FindOptions().limit(1));
      if (isNotEmpty(lastSuccessfulRecords)) {
        return successfulExecution;
      }
    }
    logger.warn("Could not get a successful workflow to find control nodes");
    return null;
  }

  @Override
  public List<String> getLastSuccessfulWorkflowExecutionIds(String workflowId) {
    final PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
                                                           .addOrder("createdAt", OrderType.DESC)
                                                           .build();

    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, false);
    final List<String> workflowExecutionIds = new ArrayList<>();

    if (workflowExecutions != null) {
      for (WorkflowExecution workflowExecution : workflowExecutions) {
        workflowExecutionIds.add(workflowExecution.getUuid());
      }
    }
    return workflowExecutionIds;
  }

  private RiskLevel getRiskLevel(int risk) {
    RiskLevel riskLevel;
    switch (risk) {
      case -1:
        riskLevel = RiskLevel.NA;
        break;
      case 0:
        riskLevel = RiskLevel.LOW;
        break;
      case 1:
        riskLevel = RiskLevel.MEDIUM;
        break;
      case 2:
        riskLevel = RiskLevel.HIGH;
        break;
      default:
        throw new RuntimeException("Unknown risk level " + risk);
    }
    return riskLevel;
  }

  @Override
  public List<NewRelicMetricHostAnalysisValue> getToolTip(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName) {
    /* Ignore analysisMinutue. Leaving it as a parameter since UI sends it.
       Fetch the latest */
    Query<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecordQuery =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("workflowExecutionId")
            .equal(workflowExecutionId)
            .order("-analysisMinute");

    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord =
        wingsPersistence.executeGetOneQuery(timeSeriesMLAnalysisRecordQuery);
    if (timeSeriesMLAnalysisRecord == null) {
      return null;
    }

    Map<String, String> txnNameToIdMap = new HashMap<>();

    for (Entry<String, TimeSeriesMLTxnSummary> txnSummaryEntry :
        timeSeriesMLAnalysisRecord.getTransactions().entrySet()) {
      txnNameToIdMap.put(txnSummaryEntry.getValue().getTxn_name(), txnSummaryEntry.getKey());
    }

    if (!txnNameToIdMap.containsKey(transactionName)) {
      return null;
    }

    TimeSeriesMLTxnSummary txnSummary =
        timeSeriesMLAnalysisRecord.getTransactions().get(txnNameToIdMap.get(transactionName));

    Map<String, String> metricNameToIdMap = new HashMap<>();
    for (Entry<String, TimeSeriesMLMetricSummary> mlMetricSummaryEntry : txnSummary.getMetrics().entrySet()) {
      metricNameToIdMap.put(mlMetricSummaryEntry.getValue().getMetric_name(), mlMetricSummaryEntry.getKey());
    }

    if (!metricNameToIdMap.containsKey(metricName)) {
      return null;
    }

    Map<String, TimeSeriesMLHostSummary> timeSeriesMLHostSummaryMap =
        txnSummary.getMetrics().get(metricNameToIdMap.get(metricName)).getResults();
    List<NewRelicMetricHostAnalysisValue> hostAnalysisValues = new ArrayList<>();

    if (timeSeriesMLHostSummaryMap != null) {
      for (Entry<String, TimeSeriesMLHostSummary> mlHostSummaryEntry : timeSeriesMLHostSummaryMap.entrySet()) {
        hostAnalysisValues.add(NewRelicMetricHostAnalysisValue.builder()
                                   .testHostName(mlHostSummaryEntry.getKey())
                                   .controlHostName(mlHostSummaryEntry.getValue().getNn())
                                   .controlValues(mlHostSummaryEntry.getValue().getControl_data())
                                   .testValues(mlHostSummaryEntry.getValue().getTest_data())
                                   .riskLevel(getRiskLevel(mlHostSummaryEntry.getValue().getRisk()))
                                   .build());
      }
    }
    return hostAnalysisValues;
  }

  @Override
  public Map<String, TimeSeriesMetricDefinition> getMetricTemplate(StateType stateType) {
    switch (stateType) {
      case NEW_RELIC:
        return NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE;
      case APP_DYNAMICS:
        return NewRelicMetricValueDefinition.APP_DYNAMICS_VALUES_TO_ANALYZE;
      case DYNA_TRACE:
        return DynaTraceTimeSeries.getDefinitionsToAnalyze();
      default:
        return null;
    }
  }

  @Override
  public NewRelicMetricAnalysisRecord getMetricsAnalysis(
      StateType stateType, String stateExecutionId, String workflowExecutionId) {
    NewRelicMetricAnalysisRecord analysisRecord;

    Query<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecordQuery =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("workflowExecutionId")
            .equal(workflowExecutionId);

    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord =
        wingsPersistence.executeGetOneQuery(timeSeriesMLAnalysisRecordQuery);
    if (timeSeriesMLAnalysisRecord != null && timeSeriesMLAnalysisRecord.getTransactions() != null) {
      List<NewRelicMetricAnalysis> metricAnalysisList = new ArrayList<>();
      for (TimeSeriesMLTxnSummary txnSummary : timeSeriesMLAnalysisRecord.getTransactions().values()) {
        List<NewRelicMetricAnalysisValue> metricsList = new ArrayList<>();
        RiskLevel globalRisk = RiskLevel.NA;
        for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
          RiskLevel riskLevel = getRiskLevel(mlMetricSummary.getMax_risk());

          if (riskLevel.compareTo(globalRisk) < 0) {
            globalRisk = riskLevel;
          }
          metricsList.add(NewRelicMetricAnalysisValue.builder()
                              .name(mlMetricSummary.getMetric_name())
                              .riskLevel(riskLevel)
                              .controlValue(mlMetricSummary.getControl_avg())
                              .testValue(mlMetricSummary.getTest_avg())
                              .build());
        }
        metricAnalysisList.add(NewRelicMetricAnalysis.builder()
                                   .metricName(txnSummary.getTxn_name())
                                   .metricValues(metricsList)
                                   .riskLevel(globalRisk)
                                   .build());
      }
      analysisRecord = NewRelicMetricAnalysisRecord.builder()
                           .applicationId(timeSeriesMLAnalysisRecord.getApplicationId())
                           .stateType(stateType)
                           .analysisMinute(timeSeriesMLAnalysisRecord.getAnalysisMinute())
                           .metricAnalyses(metricAnalysisList)
                           .stateExecutionId(timeSeriesMLAnalysisRecord.getStateExecutionId())
                           .workflowExecutionId(timeSeriesMLAnalysisRecord.getWorkflowExecutionId())
                           .showTimeSeries(true)
                           .build();

    } else {
      Query<NewRelicMetricAnalysisRecord> metricAnalysisRecordQuery =
          wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
              .field("stateExecutionId")
              .equal(stateExecutionId)
              .field("workflowExecutionId")
              .equal(workflowExecutionId);

      analysisRecord = wingsPersistence.executeGetOneQuery(metricAnalysisRecordQuery);
    }

    if (analysisRecord == null) {
      return NewRelicMetricAnalysisRecord.builder()
          .showTimeSeries(false)
          .stateType(StateType.DYNA_TRACE)
          .riskLevel(RiskLevel.NA)
          .message("No data available")
          .build();
    }

    if (analysisRecord.getMetricAnalyses() != null) {
      int highRisk = 0;
      int mediumRisk = 0;
      for (NewRelicMetricAnalysis metricAnalysis : analysisRecord.getMetricAnalyses()) {
        final RiskLevel riskLevel = metricAnalysis.getRiskLevel();
        switch (riskLevel) {
          case HIGH:
            highRisk++;
            break;
          case MEDIUM:
            mediumRisk++;
            break;
          case NA:
            noop();
            break;
          case LOW:
            noop();
            break;
          default:
            unhandled(riskLevel);
        }
      }

      if (highRisk == 0 && mediumRisk == 0) {
        analysisRecord.setMessage("No problems found");
      } else {
        String message = "";
        if (highRisk > 0) {
          message = highRisk + " high risk " + (highRisk > 1 ? "transactions" : "transaction") + " found. ";
        }

        if (mediumRisk > 0) {
          message += mediumRisk + " medium risk " + (mediumRisk > 1 ? "transactions" : "transaction") + " found.";
        }

        analysisRecord.setMessage(message);
      }

      if (highRisk > 0) {
        analysisRecord.setRiskLevel(RiskLevel.HIGH);
      } else if (mediumRisk > 0) {
        analysisRecord.setRiskLevel(RiskLevel.MEDIUM);
      } else {
        analysisRecord.setRiskLevel(RiskLevel.LOW);
      }

      Collections.sort(analysisRecord.getMetricAnalyses());
    } else {
      analysisRecord.setRiskLevel(RiskLevel.NA);
      analysisRecord.setMessage("No data available");
    }
    return analysisRecord;
  }

  @Override
  public boolean isStateValid(String appdId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appdId, stateExecutionID);
    return stateExecutionInstance != null && !stateExecutionInstance.getStatus().isFinalStatus();
  }

  @Override
  public NewRelicMetricDataRecord getLastHeartBeat(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .field("stateType")
                                                            .equal(stateType)
                                                            .field("workflowExecutionId")
                                                            .equal(workflowExecutionId)
                                                            .field("stateExecutionId")
                                                            .equal(stateExecutionId)
                                                            .field("serviceId")
                                                            .equal(serviceId)
                                                            .field("level")
                                                            .in(Lists.newArrayList(ClusterLevel.HF))
                                                            .order("-dataCollectionMinute")
                                                            .get(new FindOptions().limit(1));

    if (newRelicMetricDataRecord == null) {
      logger.info(
          "No heartbeat record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}",
          ClusterLevel.HF, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }

    return newRelicMetricDataRecord;
  }

  @Override
  public NewRelicMetricDataRecord getAnalysisMinute(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .field("stateType")
                                                            .equal(stateType)
                                                            .field("workflowExecutionId")
                                                            .equal(workflowExecutionId)
                                                            .field("stateExecutionId")
                                                            .equal(stateExecutionId)
                                                            .field("serviceId")
                                                            .equal(serviceId)
                                                            .field("level")
                                                            .in(Lists.newArrayList(ClusterLevel.H0))
                                                            .order("-dataCollectionMinute")
                                                            .get(new FindOptions().limit(1));

    if (newRelicMetricDataRecord == null) {
      logger.info(
          "No metric record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}.",
          ClusterLevel.H0, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }

    return newRelicMetricDataRecord;
  }

  @Override
  public void bumpCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId, int analysisMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionId)
                                                .field("stateExecutionId")
                                                .equal(stateExecutionId)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .equal(ClusterLevel.H0)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);

    UpdateResults updateResults = wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(NewRelicMetricDataRecord.class).set("level", ClusterLevel.HF));
    logger.info("bumpCollectionMinuteToProcess updated results size {}, for stateExecutionId {} ",
        updateResults.getUpdatedCount(), stateExecutionId);
  }

  @Override
  public void cleanUpForMetricRetry(String stateExecutionId) {
    // delete new relic metric records
    wingsPersistence.delete(
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class).field("stateExecutionId").equal(stateExecutionId));

    // delete new relic analysis records
    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                .field("stateExecutionId")
                                .equal(stateExecutionId));

    // delete time series analysis records
    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .field("stateExecutionId")
                                .equal(stateExecutionId));

    // delete time series scores records
    wingsPersistence.delete(
        wingsPersistence.createQuery(TimeSeriesMLScores.class).field("stateExecutionId").equal(stateExecutionId));

    // delete cv dashboard execution data
    wingsPersistence.delete(wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
                                .field("stateExecutionId")
                                .equal(stateExecutionId));

    // delete learning engine tasks
    wingsPersistence.delete(wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                .field("state_execution_id")
                                .equal(stateExecutionId));
  }
}
