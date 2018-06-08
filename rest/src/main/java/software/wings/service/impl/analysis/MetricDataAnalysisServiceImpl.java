package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.dl.HQuery.excludeCount;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
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
  @Inject protected SettingsService settingsService;

  @Override
  public boolean saveMetricData(String accountId, String appId, String stateExecutionId, String delegateTaskId,
      List<NewRelicMetricDataRecord> metricData) throws IOException {
    if (!isStateValid(appId, stateExecutionId)) {
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
  public boolean saveAnalysisRecords(NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                .filter("workflowExecutionId", metricAnalysisRecord.getWorkflowExecutionId())
                                .filter("stateExecutionId", metricAnalysisRecord.getStateExecutionId())
                                .filter("groupName", metricAnalysisRecord.getGroupName()));

    wingsPersistence.save(metricAnalysisRecord);
    if (logger.isDebugEnabled()) {
      logger.debug("inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: "
          + metricAnalysisRecord.getWorkflowExecutionId()
          + " StateExecutionInstanceId: " + metricAnalysisRecord.getStateExecutionId());
    }
    return true;
  }

  @Override
  public boolean saveAnalysisRecordsML(StateType stateType, String accountId, String appId, String stateExecutionId,
      final String workflowExecutionId, final String workflowId, String serviceId, String groupName,
      Integer analysisMinute, String taskId, String baseLineExecutionId,
      TimeSeriesMLAnalysisRecord mlAnalysisResponse) {
    mlAnalysisResponse.setStateType(stateType);
    mlAnalysisResponse.setAppId(appId);
    mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setAnalysisMinute(analysisMinute);
    mlAnalysisResponse.setBaseLineExecutionId(baseLineExecutionId);
    mlAnalysisResponse.setGroupName(groupName);

    if (isEmpty(mlAnalysisResponse.getGroupName())) {
      mlAnalysisResponse.setGroupName(DEFAULT_GROUP_NAME);
    }

    TimeSeriesMLScores timeSeriesMLScores = TimeSeriesMLScores.builder()
                                                .appId(appId)
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
    bumpCollectionMinuteToProcess(
        stateType, appId, stateExecutionId, workflowExecutionId, serviceId, groupName, analysisMinute);
    learningEngineService.markCompleted(taskId);

    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .filter("appId", appId)
                                .filter("workflowExecutionId", workflowExecutionId)
                                .filter("stateExecutionId", stateExecutionId)
                                .filter("groupName", groupName));

    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                .filter("appId", appId)
                                .filter("workflowExecutionId", workflowExecutionId)
                                .filter("stateExecutionId", stateExecutionId)
                                .filter("groupName", groupName));

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
      String appId, String workflowId, int analysisMinute, int limit) {
    List<String> workflowExecutionIds = getLastSuccessfulWorkflowExecutionIds(workflowId);
    return wingsPersistence.createQuery(TimeSeriesMLScores.class)
        .filter("workflowId", workflowId)
        .filter("appId", appId)
        .filter("analysisMinute", analysisMinute)
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
  public List<NewRelicMetricDataRecord> getRecords(StateType stateType, String appId, String workflowExecutionId,
      String stateExecutionId, String workflowId, String serviceId, String groupName, Set<String> nodes,
      int analysisMinute, int analysisStartMinute) {
    return wingsPersistence.createQuery(NewRelicMetricDataRecord.class, excludeCount)
        .filter("stateType", stateType)
        .filter("appId", appId)
        .filter("workflowId", workflowId)
        .filter("workflowExecutionId", workflowExecutionId)
        .filter("stateExecutionId", stateExecutionId)
        .filter("serviceId", serviceId)
        .filter("groupName", groupName)
        .field("host")
        .hasAnyOf(nodes)
        .field("level")
        .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
        .field("dataCollectionMinute")
        .lessThanOrEq(analysisMinute)
        .field("dataCollectionMinute")
        .greaterThanOrEq(analysisStartMinute)
        .asList();
  }

  @Override
  public List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(StateType stateType, String appId,
      String workflowId, String workflowExecutionID, String serviceId, String groupName, int analysisMinute,
      int analysisStartMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .filter("stateType", stateType)
                                                .filter("appId", appId)
                                                .filter("workflowId", workflowId)
                                                .filter("workflowExecutionId", workflowExecutionID)
                                                .filter("serviceId", serviceId)
                                                .field("groupName")
                                                .in(asList(groupName, NewRelicMetricDataRecord.DEFAULT_GROUP_NAME))
                                                .field("level")
                                                .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute)
                                                .field("dataCollectionMinute")
                                                .greaterThanOrEq(analysisStartMinute);
    return query.asList();
  }

  @Override
  public int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName) {
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
            .filter("stateType", stateType)
            .filter("appId", appId)
            .filter("workflowId", workflowId)
            .filter("workflowExecutionId", workflowExecutionId)
            .filter("serviceId", serviceId)
            .field("groupName")
            .in(asList(groupName, NewRelicMetricDataRecord.DEFAULT_GROUP_NAME))
            .field("level")
            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
            .order("-dataCollectionMinute")
            .get();

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowId", workflowId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .filter("groupName", groupName)
                                                            .field("level")
                                                            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                            .order("dataCollectionMinute")
                                                            .get();

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId) {
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(workflowId);
    for (String successfulExecution : successfulExecutions) {
      if (wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
              .filter("stateType", stateType)
              .filter("appId", appId)
              .filter("workflowId", workflowId)
              .filter("workflowExecutionId", successfulExecution)
              .filter("serviceId", serviceId)
              .field("level")
              .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
              .count(new CountOptions().limit(1))
          > 0) {
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
      int analysisMinute, String transactionName, String metricName, String groupName) {
    /* Ignore analysisMinutue. Leaving it as a parameter since UI sends it.
       Fetch the latest */
    groupName = isEmpty(groupName) ? NewRelicMetricDataRecord.DEFAULT_GROUP_NAME : groupName;
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("stateExecutionId", stateExecutionId)
            .filter("workflowExecutionId", workflowExecutionId)
            .filter("groupName", groupName)
            .order("-analysisMinute")
            .get();
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
  public Map<String, TimeSeriesMetricDefinition> getMetricTemplate(StateType stateType, String stateExecutionId) {
    switch (stateType) {
      case NEW_RELIC:
        return NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE;
      case APP_DYNAMICS:
        return NewRelicMetricValueDefinition.APP_DYNAMICS_VALUES_TO_ANALYZE;
      case DYNA_TRACE:
        return DynaTraceTimeSeries.getDefinitionsToAnalyze();
      case PROMETHEUS:
      case CLOUD_WATCH:
      case DATA_DOG:
      case APM_VERIFICATION:
        return getMetricTemplates(stateType, stateExecutionId);
      default:
        throw new WingsException("Invalid Verification StateType.");
    }
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  public List<NewRelicMetricAnalysisRecord> getMetricsAnalysisForDemo(
      String appId, String stateExecutionId, String workflowExecutionId) {
    logger.info("Creating analysis summary for demo {}", stateExecutionId);
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.createQuery(StateExecutionInstance.class).field("_id").equal(stateExecutionId).get();
    if (stateExecutionId == null) {
      logger.error("State execution instance not found for {}", stateExecutionId);
      return null;
    }

    SettingAttribute settingAttribute = settingsService.get(
        ((MetricAnalysisExecutionData) stateExecutionInstance.getStateExecutionData()).getServerConfigId());

    if (stateExecutionInstance.getStateType().equals(StateType.NEW_RELIC.name())) {
      if (settingAttribute.getName().toLowerCase().endsWith("dev")
          || settingAttribute.getName().toLowerCase().endsWith("prod")) {
        if (stateExecutionInstance.getStatus() == ExecutionStatus.SUCCESS) {
          return getMetricsAnalysis(
              "CV-Demo-" + stateExecutionInstance.getStateType(), "CV-Demo-TS-Success", "CV-Demo");
        } else {
          return getMetricsAnalysis(
              "CV-Demo-" + stateExecutionInstance.getStateType(), "CV-Demo-TS-Failure", "CV-Demo");
        }
      }
    }

    return getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
  }

  @Override
  public List<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      String appId, String stateExecutionId, String workflowExecutionId) {
    List<NewRelicMetricAnalysisRecord> analysisRecords = new ArrayList<>();
    List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("appId", appId)
            .filter("stateExecutionId", stateExecutionId)
            .filter("workflowExecutionId", workflowExecutionId)
            .asList();

    timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
      if (timeSeriesMLAnalysisRecord.getTransactions() != null) {
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
                                     .tag(txnSummary.getTxn_tag())
                                     .metricValues(metricsList)
                                     .riskLevel(globalRisk)
                                     .build());
        }
        analysisRecords.add(NewRelicMetricAnalysisRecord.builder()
                                .appId(timeSeriesMLAnalysisRecord.getAppId())
                                .stateType(timeSeriesMLAnalysisRecord.getStateType())
                                .analysisMinute(timeSeriesMLAnalysisRecord.getAnalysisMinute())
                                .metricAnalyses(metricAnalysisList)
                                .stateExecutionId(timeSeriesMLAnalysisRecord.getStateExecutionId())
                                .workflowExecutionId(timeSeriesMLAnalysisRecord.getWorkflowExecutionId())
                                .baseLineExecutionId(timeSeriesMLAnalysisRecord.getBaseLineExecutionId())
                                .showTimeSeries(true)
                                .groupName(timeSeriesMLAnalysisRecord.getGroupName())
                                .build());
      }
    });

    analysisRecords.addAll(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                               .filter("appId", appId)
                               .filter("stateExecutionId", stateExecutionId)
                               .filter("workflowExecutionId", workflowExecutionId)
                               .asList());

    if (isEmpty(analysisRecords)) {
      analysisRecords.add(NewRelicMetricAnalysisRecord.builder()
                              .showTimeSeries(false)
                              .stateType(StateType.APP_DYNAMICS)
                              .riskLevel(RiskLevel.NA)
                              .message("No data available")
                              .build());
    }

    Map<String, TimeSeriesMlAnalysisGroupInfo> metricGroups = getMetricGroups(appId, stateExecutionId);
    analysisRecords.forEach(analysisRecord -> {
      TimeSeriesMlAnalysisGroupInfo mlAnalysisGroupInfo = metricGroups.get(analysisRecord.getGroupName());
      analysisRecord.setDependencyPath(mlAnalysisGroupInfo == null ? null : mlAnalysisGroupInfo.getDependencyPath());
      analysisRecord.setMlAnalysisType(
          mlAnalysisGroupInfo == null ? TimeSeriesMlAnalysisType.COMPARATIVE : mlAnalysisGroupInfo.getMlAnalysisType());
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

      if (analysisRecord.getStateType() == StateType.DYNA_TRACE) {
        for (NewRelicMetricAnalysis analysis : analysisRecord.getMetricAnalyses()) {
          String metricName = analysis.getMetricName();
          String[] split = metricName.split(":");
          if (split == null || split.length == 1) {
            analysis.setDisplayName(metricName);
            analysis.setFullMetricName(metricName);
            continue;
          }
          String btName = split[0];
          String fullBTName = btName + " (" + metricName.substring(btName.length() + 1) + ")";
          analysis.setDisplayName(btName);
          analysis.setFullMetricName(fullBTName);
        }
      }
    });
    Collections.sort(analysisRecords);
    return analysisRecords;
  }

  @Override
  public boolean isStateValid(String appId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appId, stateExecutionID);
    return stateExecutionInstance != null && !ExecutionStatus.isFinalStatus(stateExecutionInstance.getStatus());
  }

  @Override
  public NewRelicMetricDataRecord getLastHeartBeat(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("stateExecutionId", stateExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .filter("groupName", groupName)
                                                            .field("level")
                                                            .in(Lists.newArrayList(ClusterLevel.HF))
                                                            .order("-dataCollectionMinute")
                                                            .get();

    if (newRelicMetricDataRecord == null) {
      logger.info(
          "No heartbeat record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}",
          ClusterLevel.HF, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }

    return newRelicMetricDataRecord;
  }

  @Override
  public NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName) {
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("stateExecutionId", stateExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .filter("groupName", groupName)
                                                            .field("level")
                                                            .in(Lists.newArrayList(ClusterLevel.H0))
                                                            .order("-dataCollectionMinute")
                                                            .get();

    if (newRelicMetricDataRecord == null) {
      logger.info(
          "No metric record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}.",
          ClusterLevel.H0, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }

    return newRelicMetricDataRecord;
  }

  @Override
  public void bumpCollectionMinuteToProcess(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName, int analysisMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .filter("stateType", stateType)
                                                .filter("appId", appId)
                                                .filter("workflowExecutionId", workflowExecutionId)
                                                .filter("stateExecutionId", stateExecutionId)
                                                .filter("serviceId", serviceId)
                                                .filter("groupName", groupName)
                                                .filter("level", ClusterLevel.H0)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);

    UpdateResults updateResults = wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(NewRelicMetricDataRecord.class).set("level", ClusterLevel.HF));
    logger.info("bumpCollectionMinuteToProcess updated results size {}, for stateExecutionId {} ",
        updateResults.getUpdatedCount(), stateExecutionId);
  }

  @Override
  public void saveMetricTemplates(
      StateType stateType, String stateExecutionId, Map<String, TimeSeriesMetricDefinition> metricTemplates) {
    wingsPersistence.save(TimeSeriesMetricTemplates.builder()
                              .stateType(stateType)
                              .stateExecutionId(stateExecutionId)
                              .metricTemplates(metricTemplates)
                              .build());
  }

  @Override
  public Map<String, TimeSeriesMetricDefinition> getMetricTemplates(StateType stateType, String stateExecutionId) {
    TimeSeriesMetricTemplates newRelicMetricDataRecord = wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
                                                             .field("stateType")
                                                             .equal(stateType)
                                                             .field("stateExecutionId")
                                                             .equal(stateExecutionId)
                                                             .get();
    return newRelicMetricDataRecord == null ? null : newRelicMetricDataRecord.getMetricTemplates();
  }

  @Override
  public void saveMetricGroups(
      String appId, StateType stateType, String stateExecutionId, Map<String, TimeSeriesMlAnalysisGroupInfo> groups) {
    wingsPersistence.save(TimeSeriesMetricGroup.builder()
                              .appId(appId)
                              .stateType(stateType)
                              .stateExecutionId(stateExecutionId)
                              .groups(groups)
                              .build());
  }

  @Override
  public Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId) {
    TimeSeriesMetricGroup timeSeriesMetricGroup = wingsPersistence.createQuery(TimeSeriesMetricGroup.class)
                                                      .field("stateExecutionId")
                                                      .equal(stateExecutionId)
                                                      .field("appId")
                                                      .equal(appId)
                                                      .get();

    return timeSeriesMetricGroup == null ? new ImmutableMap.Builder<String, TimeSeriesMlAnalysisGroupInfo>()
                                               .put(DEFAULT_GROUP_NAME,
                                                   TimeSeriesMlAnalysisGroupInfo.builder()
                                                       .groupName(DEFAULT_GROUP_NAME)
                                                       .dependencyPath(DEFAULT_GROUP_NAME)
                                                       .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                                                       .build())
                                               .build()
                                         : timeSeriesMetricGroup.getGroups();
  }

  @Override
  public void cleanUpForMetricRetry(String stateExecutionId) {
    // delete the metric templates
    wingsPersistence.delete(
        wingsPersistence.createQuery(TimeSeriesMetricTemplates.class).filter("stateExecutionId", stateExecutionId));

    // delete new relic metric records
    wingsPersistence.delete(
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class).filter("stateExecutionId", stateExecutionId));

    // delete new relic analysis records
    wingsPersistence.delete(
        wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class).filter("stateExecutionId", stateExecutionId));

    // delete time series analysis records
    wingsPersistence.delete(
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).filter("stateExecutionId", stateExecutionId));

    // delete time series scores records
    wingsPersistence.delete(
        wingsPersistence.createQuery(TimeSeriesMLScores.class).filter("stateExecutionId", stateExecutionId));

    // delete cv dashboard execution data
    wingsPersistence.delete(wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
                                .filter("stateExecutionId", stateExecutionId));

    // delete learning engine tasks
    wingsPersistence.delete(
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("state_execution_id", stateExecutionId));
  }
}
