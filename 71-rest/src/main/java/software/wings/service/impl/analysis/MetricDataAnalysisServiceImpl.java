package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.delegatetasks.AppdynamicsDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.utils.Misc.replaceDotWithUnicode;
import static software.wings.utils.Misc.replaceUnicodeWithDot;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
  @Inject private WorkflowService workflowService;

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId) {
    List<String> successfulExecutions =
        workflowService.getLastSuccessfulWorkflowExecutionIds(appId, workflowId, serviceId);
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
        throw new IllegalArgumentException("Unknown risk level " + risk);
    }
    return riskLevel;
  }

  @Override
  public List<NewRelicMetricHostAnalysisValue> getToolTip(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName, String groupName) {
    /* Ignore analysisMinutue. Leaving it as a parameter since UI sends it.
       Fetch the latest */
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("stateExecutionId", stateExecutionId)
            .filter("workflowExecutionId", workflowExecutionId)
            .filter("groupName", isEmpty(groupName) ? NewRelicMetricDataRecord.DEFAULT_GROUP_NAME : groupName)
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
        boolean isPredictiveAnalysis = false;
        if (mlHostSummaryEntry.getValue().getTimeSeriesMlAnalysisType() != null) {
          isPredictiveAnalysis =
              mlHostSummaryEntry.getValue().getTimeSeriesMlAnalysisType().equals(TimeSeriesMlAnalysisType.PREDICTIVE);
        }
        hostAnalysisValues.add(
            NewRelicMetricHostAnalysisValue.builder()
                .testHostName(mlHostSummaryEntry.getKey())
                .controlHostName(mlHostSummaryEntry.getValue().getNn())
                .controlValues(mlHostSummaryEntry.getValue().getControl_data())
                .testValues(mlHostSummaryEntry.getValue().getTest_data())
                .riskLevel(getRiskLevel(mlHostSummaryEntry.getValue().getRisk()))
                .testStartIndex(isPredictiveAnalysis ? PREDECTIVE_HISTORY_MINUTES : -1)
                .anomalies(isPredictiveAnalysis ? mlHostSummaryEntry.getValue().getAnomalies() : null)
                .build());
      }
    }
    return hostAnalysisValues;
  }

  @Override
  public boolean saveCustomThreshold(String appId, StateType stateType, String serviceId, String groupName,
      String transactionName, TimeSeriesMetricDefinition metricDefinition) {
    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
                                .filter("appId", appId)
                                .filter("stateType", stateType)
                                .filter("serviceId", serviceId)
                                .filter("groupName", groupName)
                                .filter("transactionName", transactionName)
                                .filter("metricName", metricDefinition.getMetricName()));
    TimeSeriesMLTransactionThresholds timeSeriesMLTransactionThresholds =
        TimeSeriesMLTransactionThresholds.builder()
            .stateType(stateType)
            .groupName(groupName)
            .serviceId(serviceId)
            .transactionName(transactionName)
            .metricName(metricDefinition.getMetricName())
            .thresholds(metricDefinition)
            .build();
    timeSeriesMLTransactionThresholds.setAppId(appId);
    wingsPersistence.save(timeSeriesMLTransactionThresholds);
    return true;
  }

  @Override
  public TimeSeriesMLTransactionThresholds getCustomThreshold(String appId, StateType stateType, String serviceId,
      String groupName, String transactionName, String metricName) {
    return wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
        .filter("appId", appId)
        .filter("stateType", stateType)
        .filter("serviceId", serviceId)
        .filter("groupName", groupName)
        .filter("transactionName", transactionName)
        .filter("metricName", metricName)
        .get();
  }

  @Override
  public boolean deleteCustomThreshold(String appId, StateType stateType, String serviceId, String groupName,
      String transactionName, String metricName) {
    return wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
                                       .filter("appId", appId)
                                       .filter("stateType", stateType)
                                       .filter("serviceId", serviceId)
                                       .filter("groupName", groupName)
                                       .filter("transactionName", transactionName)
                                       .filter("metricName", metricName));
  }

  public List<NewRelicMetricAnalysisRecord> getMetricsAnalysisForDemo(
      final String appId, final String stateExecutionId, final String workflowExecutionId) {
    logger.info("Creating analysis summary for demo {}", stateExecutionId);
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.createQuery(StateExecutionInstance.class).field("_id").equal(stateExecutionId).get();
    if (stateExecutionInstance == null) {
      logger.error("State execution instance not found for {}", stateExecutionId);
      throw new WingsException(ErrorCode.STATE_EXECUTION_INSTANCE_NOT_FOUND, stateExecutionId);
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
      final String appId, final String stateExecutionId, final String workflowExecutionId) {
    List<NewRelicMetricAnalysisRecord> analysisRecords = new ArrayList<>();
    List<TimeSeriesMLAnalysisRecord> allAnalysisRecords = wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                                              .filter("appId", appId)
                                                              .filter("stateExecutionId", stateExecutionId)
                                                              .filter("workflowExecutionId", workflowExecutionId)
                                                              .order(Sort.descending("createdAt"))
                                                              .asList();

    Map<String, TimeSeriesMLAnalysisRecord> groupVsAnalysisRecord = new HashMap<>();
    allAnalysisRecords.forEach(analysisRecord -> {
      if (!groupVsAnalysisRecord.containsKey(analysisRecord.getGroupName())) {
        groupVsAnalysisRecord.put(analysisRecord.getGroupName(), analysisRecord);
      }
    });
    Collection<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = groupVsAnalysisRecord.values();
    timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
      NewRelicMetricAnalysisRecord metricAnalysisRecord =
          NewRelicMetricAnalysisRecord.builder()
              .appId(timeSeriesMLAnalysisRecord.getAppId())
              .stateType(timeSeriesMLAnalysisRecord.getStateType())
              .analysisMinute(timeSeriesMLAnalysisRecord.getAnalysisMinute())
              .stateExecutionId(timeSeriesMLAnalysisRecord.getStateExecutionId())
              .workflowExecutionId(timeSeriesMLAnalysisRecord.getWorkflowExecutionId())
              .baseLineExecutionId(timeSeriesMLAnalysisRecord.getBaseLineExecutionId())
              .showTimeSeries(true)
              .groupName(timeSeriesMLAnalysisRecord.getGroupName())
              .message(timeSeriesMLAnalysisRecord.getMessage())
              .build();
      analysisRecords.add(metricAnalysisRecord);
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
                                .type(mlMetricSummary.getMetric_type())
                                .alertType(mlMetricSummary.getAlert_type())
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
        metricAnalysisRecord.setMetricAnalyses(metricAnalysisList);
      }
    });
    List<NewRelicMetricAnalysisRecord> allMetricAnalyisRecords =
        wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
            .filter("appId", appId)
            .filter("stateExecutionId", stateExecutionId)
            .filter("workflowExecutionId", workflowExecutionId)
            .order(Sort.descending("createdAt"))
            .asList();

    Map<String, NewRelicMetricAnalysisRecord> groupVsMetricAnalysisRecord = new HashMap<>();
    allMetricAnalyisRecords.forEach(analysisRecord -> {
      if (!groupVsMetricAnalysisRecord.containsKey(analysisRecord.getGroupName())) {
        groupVsMetricAnalysisRecord.put(analysisRecord.getGroupName(), analysisRecord);
      }
    });
    analysisRecords.addAll(groupVsMetricAnalysisRecord.values());

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
          StringBuffer message = new StringBuffer(20);
          if (highRisk > 0) {
            message.append(highRisk + " high risk " + (highRisk > 1 ? "transactions" : "transaction") + " found. ");
          }

          if (mediumRisk > 0) {
            message.append(
                mediumRisk + " medium risk " + (mediumRisk > 1 ? "transactions" : "transaction") + " found.");
          }

          analysisRecord.setMessage(message.toString());
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
      }

      if (analysisRecord.getStateType() == StateType.DYNA_TRACE && !isEmpty(analysisRecord.getMetricAnalyses())) {
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
  public boolean isStateValid(String appId, String stateExecutionId) {
    return workflowService.isStateValid(appId, stateExecutionId);
  }

  @Override
  public void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId, String cvConfigId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates) {
    TimeSeriesMetricTemplates metricTemplate = TimeSeriesMetricTemplates.builder()
                                                   .stateType(stateType)
                                                   .stateExecutionId(stateExecutionId)
                                                   .metricTemplates(metricTemplates)
                                                   .cvConfigId(cvConfigId)
                                                   .build();
    metricTemplate.setAppId(appId);
    wingsPersistence.save(metricTemplate);
  }

  @Override
  public void saveMetricGroups(
      String appId, StateType stateType, String stateExecutionId, Map<String, TimeSeriesMlAnalysisGroupInfo> groups) {
    Map<String, TimeSeriesMlAnalysisGroupInfo> toSave = new HashMap<>();
    groups.forEach((groupName, timeSeriesMlAnalysisGroupInfo) -> {
      groupName = replaceDotWithUnicode(groupName);
      timeSeriesMlAnalysisGroupInfo.setGroupName(groupName);
      toSave.put(groupName, timeSeriesMlAnalysisGroupInfo);
    });
    logger.info("Creating groups for stateExecutionId {}, appId {}, stateType {}, groups {}", stateExecutionId, appId,
        stateType, toSave);
    wingsPersistence.save(TimeSeriesMetricGroup.builder()
                              .appId(appId)
                              .stateType(stateType)
                              .stateExecutionId(stateExecutionId)
                              .groups(toSave)
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

    if (timeSeriesMetricGroup != null) {
      Map<String, TimeSeriesMlAnalysisGroupInfo> toReturn = new HashMap<>();
      timeSeriesMetricGroup.getGroups().forEach((groupName, timeSeriesMlAnalysisGroupInfo) -> {
        groupName = replaceUnicodeWithDot(groupName);
        timeSeriesMlAnalysisGroupInfo.setGroupName(groupName);
        toReturn.put(groupName, timeSeriesMlAnalysisGroupInfo);
      });

      return toReturn;
    }

    return new ImmutableMap.Builder<String, TimeSeriesMlAnalysisGroupInfo>()
        .put(DEFAULT_GROUP_NAME,
            TimeSeriesMlAnalysisGroupInfo.builder()
                .groupName(DEFAULT_GROUP_NAME)
                .dependencyPath(DEFAULT_GROUP_NAME)
                .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                .build())
        .build();
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

    // delete the metric groups
    wingsPersistence.delete(
        wingsPersistence.createQuery(TimeSeriesMetricGroup.class).filter("stateExecutionId", stateExecutionId));
  }
}
