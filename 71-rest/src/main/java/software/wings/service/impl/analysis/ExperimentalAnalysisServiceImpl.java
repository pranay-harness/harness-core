package software.wings.service.impl.analysis;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.metrics.RiskLevel.HIGH;
import static software.wings.metrics.RiskLevel.LOW;
import static software.wings.metrics.RiskLevel.MEDIUM;
import static software.wings.metrics.RiskLevel.NA;
import static software.wings.metrics.RiskLevel.getRiskLevel;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord.ExperimentalLogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.ExperimentalMessageComparisonResult.ExperimentalMessageComparisonResultKeys;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord.ExperimentalMetricAnalysisRecordKeys;
import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;
import software.wings.service.impl.newrelic.ExperimentalMetricRecord;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ExperimentalAnalysisService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Slf4j
public class ExperimentalAnalysisServiceImpl implements ExperimentalAnalysisService {
  private static final double HIGH_RISK_THRESHOLD = 50;
  private static final double MEDIUM_RISK_THRESHOLD = 25;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private AnalysisService analysisService;

  @Inject private DataStoreService dataStoreService;

  @Override
  public ExperimentPerformance getMetricExpAnalysisPerformance(
      PageRequest<ExperimentalMetricAnalysisRecord> pageRequest) {
    pageRequest.addFieldsIncluded(ExperimentalMetricAnalysisRecordKeys.experimentStatus)
        .addFilter(ExperimentalMetricAnalysisRecordKeys.experimentStatus, EQ, ExperimentStatus.SUCCESS)
        .addFilter(MetricAnalysisRecordKeys.analysisMinute, SearchFilter.Operator.EQ, 0);

    long success =
        wingsPersistence.query(ExperimentalMetricAnalysisRecord.class, pageRequest, excludeAuthority).getTotal();

    pageRequest.addFieldsIncluded(ExperimentalMetricAnalysisRecordKeys.experimentStatus)
        .addFilter(ExperimentalMetricAnalysisRecordKeys.experimentStatus, EQ, ExperimentStatus.FAILURE)
        .addFilter(MetricAnalysisRecordKeys.analysisMinute, SearchFilter.Operator.EQ, 0);

    long failure =
        wingsPersistence.query(ExperimentalMetricAnalysisRecord.class, pageRequest, excludeAuthority).getTotal();

    pageRequest.addFieldsIncluded(ExperimentalMetricAnalysisRecordKeys.experimentStatus)
        .addFilter(MetricAnalysisRecordKeys.analysisMinute, SearchFilter.Operator.EQ, 0);

    long total =
        wingsPersistence.query(ExperimentalMetricAnalysisRecord.class, pageRequest, excludeAuthority).getTotal();

    return ExperimentPerformance.builder()
        .improvementPercentage(success * 100.0 / total)
        .declinePercentage(failure * 100.0 / total)
        .build();
  }

  @Override
  public PageResponse<ExpAnalysisInfo> getMetricExpAnalysisInfoList(
      PageRequest<ExperimentalMetricAnalysisRecord> pageRequest) {
    List<ExpAnalysisInfo> result = new ArrayList<>();
    Set<String> stateExecutionIds = new HashSet<>();

    pageRequest.addOrder(MetricAnalysisRecord.BaseKeys.createdAt, SortOrder.OrderType.DESC)
        .addFieldsIncluded(MetricAnalysisRecordKeys.stateExecutionId)
        .addFieldsIncluded(MetricAnalysisRecord.BaseKeys.appId)
        .addFieldsIncluded(MetricAnalysisRecordKeys.stateType)
        .addFieldsIncluded(ExperimentalMetricAnalysisRecordKeys.experimentName)
        .addFieldsIncluded(ExperimentalMetricAnalysisRecordKeys.envId)
        .addFieldsIncluded(MetricAnalysisRecordKeys.workflowExecutionId)
        .addFieldsIncluded(MetricAnalysisRecord.BaseKeys.createdAt)
        .addFilter(MetricAnalysisRecordKeys.analysisMinute, SearchFilter.Operator.EQ, 0);

    PageResponse<ExperimentalMetricAnalysisRecord> analysisRecords =
        wingsPersistence.query(ExperimentalMetricAnalysisRecord.class, pageRequest, excludeAuthority);

    for (ExperimentalMetricAnalysisRecord record : analysisRecords) {
      if (!stateExecutionIds.contains(record.getStateExecutionId())) {
        result.add(ExpAnalysisInfo.builder()
                       .stateExecutionId(record.getStateExecutionId())
                       .appId(record.getAppId())
                       .stateType(record.getStateType())
                       .createdAt(record.getCreatedAt())
                       .expName(record.getExperimentName())
                       .envId(record.getEnvId())
                       .workflowExecutionId(record.getWorkflowExecutionId())
                       .mismatch(record.isMismatched())
                       .build());
        stateExecutionIds.add(record.getStateExecutionId());
      }
    }

    PageResponse<ExpAnalysisInfo> analysisInfo = new PageResponse<>();
    analysisInfo.setTotal(analysisRecords.getTotal());
    analysisInfo.setResponse(result);
    return analysisInfo;
  }

  @Override
  public List<ExpAnalysisInfo> getLogExpAnalysisInfoList() {
    List<ExpAnalysisInfo> result = new ArrayList<>();
    try (HIterator<ExperimentalLogMLAnalysisRecord> analysisRecords =
             new HIterator<>(wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class, excludeAuthority)
                                 .project(ExperimentalLogMLAnalysisRecordKeys.stateExecutionId, true)
                                 .project("appId", true)
                                 .project(ExperimentalLogMLAnalysisRecordKeys.stateType, true)
                                 .project(ExperimentalLogMLAnalysisRecordKeys.experiment_name, true)
                                 .project("createdAt", true)
                                 .project(ExperimentalLogMLAnalysisRecordKeys.envId, true)
                                 .project(ExperimentalLogMLAnalysisRecordKeys.workflowExecutionId, true)
                                 .fetch())) {
      while (analysisRecords.hasNext()) {
        final ExperimentalLogMLAnalysisRecord record = analysisRecords.next();
        result.add(ExpAnalysisInfo.builder()
                       .stateExecutionId(record.getStateExecutionId())
                       .appId(record.getAppId())
                       .stateType(record.getStateType())
                       .createdAt(record.getCreatedAt())
                       .expName(record.getExperiment_name())
                       .envId(record.getEnvId())
                       .workflowExecutionId(record.getWorkflowExecutionId())
                       .build());
      }
    }
    return result;
  }

  @Override
  public ExperimentalMetricRecord markExperimentStatus(
      String stateExecutionId, StateType stateType, String experimentName, ExperimentStatus experimentStatus) {
    List<ExperimentalMetricAnalysisRecord> experimentalAnalysisRecords =
        wingsPersistence.createQuery(ExperimentalMetricAnalysisRecord.class, excludeAuthority)
            .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
            .filter(ExperimentalMetricAnalysisRecordKeys.experimentName, experimentName)
            .order(Sort.descending(MetricAnalysisRecordKeys.analysisMinute))
            .asList();
    experimentalAnalysisRecords.forEach(record -> record.setExperimentStatus(experimentStatus));
    wingsPersistence.save(experimentalAnalysisRecords);
    return getExperimentalMetricAnalysisSummary(stateExecutionId, stateType, experimentName);
  }

  @Override
  public ExperimentalMetricRecord getExperimentalMetricAnalysisSummary(
      String stateExecutionId, StateType stateType, String expName) {
    ExperimentalMetricAnalysisRecord experimentalAnalysisRecord =
        wingsPersistence.createQuery(ExperimentalMetricAnalysisRecord.class, excludeAuthority)
            .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
            .order(Sort.descending(MetricAnalysisRecordKeys.analysisMinute))
            .filter(ExperimentalMetricAnalysisRecordKeys.experimentName, expName)
            .get();
    TimeSeriesMLAnalysisRecord analysisRecord =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeAuthority)
            .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
            .order(Sort.descending(MetricAnalysisRecordKeys.analysisMinute))
            .get();

    return getExperimentalMetricAnalysisSummary(
        stateExecutionId, stateType, experimentalAnalysisRecord, analysisRecord);
  }

  @Override
  public void updateMismatchStatusForExperimentalRecord(String stateExecutionId, Integer analysisMinute) {
    try {
      if (stateExecutionId == null || analysisMinute == null) {
        logger.info("Cannot update experimental status for state execution id null");
        return;
      }
      ExperimentalMetricAnalysisRecord experimentalRecord =
          wingsPersistence.createQuery(ExperimentalMetricAnalysisRecord.class)
              .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
              .filter(MetricAnalysisRecordKeys.analysisMinute, analysisMinute)
              .get();
      TimeSeriesMLAnalysisRecord analysisRecord =
          wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
              .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
              .filter(MetricAnalysisRecordKeys.analysisMinute, analysisMinute)
              .get();

      if (analysisRecord == null || experimentalRecord == null) {
        return;
      }

      ExperimentalMetricRecord experimentalMetricRecord = getExperimentalMetricAnalysisSummary(
          stateExecutionId, analysisRecord.getStateType(), experimentalRecord, analysisRecord);

      boolean matched =
          experimentalMetricRecord.getRiskLevel().equals(experimentalMetricRecord.getExperimentalRiskLevel());

      if (matched) {
        for (ExperimentalMetricRecord.ExperimentalMetricAnalysis metricAnalysis :
            experimentalMetricRecord.getMetricAnalysis()) {
          matched = metricAnalysis.getExperimentalRiskLevel().equals(metricAnalysis.getRiskLevel());
          if (!matched) {
            break;
          }

          for (ExperimentalMetricRecord.ExperimentalMetricAnalysisValue value : metricAnalysis.getMetricValues()) {
            matched = value.getRiskLevel().equals(value.getExperimentalRiskLevel());
            if (!matched) {
              break;
            }
          }

          if (!matched) {
            break;
          }
        }
      }

      experimentalRecord.setMismatched(!matched);
      wingsPersistence.save(experimentalRecord);
    } catch (Exception e) {
      logger.info("Exception while saving mismatched status for experiment {}", stateExecutionId, e);
    }
  }

  private ExperimentalMetricRecord getExperimentalMetricAnalysisSummary(String stateExecutionId, StateType stateType,
      ExperimentalMetricAnalysisRecord experimentalAnalysisRecord, TimeSeriesMLAnalysisRecord analysisRecord) {
    experimentalAnalysisRecord.decompressTransactions();

    if (analysisRecord == null) {
      logger.error("Actual analysis not done for this state execution id: {}", stateExecutionId);
      throw new WingsException("Actual analysis not done for this state execution id");
    }

    analysisRecord.decompressTransactions();

    ExperimentalMetricRecord metricRecord = ExperimentalMetricRecord.builder()
                                                .workflowExecutionId(analysisRecord.getWorkflowExecutionId())
                                                .stateExecutionId(stateExecutionId)
                                                .cvConfigId(analysisRecord.getCvConfigId())
                                                .analysisMinute(analysisRecord.getAnalysisMinute())
                                                .stateType(stateType)
                                                .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                                                .baseLineExecutionId(analysisRecord.getBaseLineExecutionId())
                                                .mismatch(experimentalAnalysisRecord.isMismatched())
                                                .experimentStatus(experimentalAnalysisRecord.getExperimentStatus())
                                                .build();

    List<ExperimentalMetricRecord.ExperimentalMetricAnalysis> metricAnalysis = new ArrayList<>();

    if (isNotEmpty(analysisRecord.getTransactions())) {
      if (isEmpty(experimentalAnalysisRecord.getTransactions())) {
        logger.error("Experimental transactions not found: {}", stateExecutionId);
        experimentalAnalysisRecord.setTransactions(new HashMap<>());
      }

      for (Map.Entry<String, TimeSeriesMLTxnSummary> txnSummaryEntry : analysisRecord.getTransactions().entrySet()) {
        TimeSeriesMLTxnSummary txnSummary = txnSummaryEntry.getValue();
        TimeSeriesMLTxnSummary experimentalTxnSummary =
            experimentalAnalysisRecord.getTransactions().get(txnSummaryEntry.getKey());

        RiskLevel globalRisk = RiskLevel.NA;
        RiskLevel globalExperimentalRisk = RiskLevel.NA;

        List<ExperimentalMetricRecord.ExperimentalMetricAnalysisValue> metricValues = new ArrayList<>();

        for (Map.Entry<String, TimeSeriesMLMetricSummary> metricSummaryEntry : txnSummary.getMetrics().entrySet()) {
          TimeSeriesMLMetricSummary metricSummary = metricSummaryEntry.getValue();
          TimeSeriesMLMetricSummary experimentalMetricSummary =
              experimentalTxnSummary == null || isEmpty(experimentalTxnSummary.getMetrics())
              ? null
              : experimentalTxnSummary.getMetrics().get(metricSummaryEntry.getKey());

          RiskLevel riskLevel = getRiskLevel(metricSummary.getMax_risk());
          RiskLevel experimentalRiskLevel =
              experimentalMetricSummary == null ? NA : getRiskLevel(experimentalMetricSummary.getMax_risk());

          if (riskLevel.compareTo(globalRisk) < 0) {
            globalRisk = riskLevel;
          }

          if (experimentalRiskLevel.compareTo(globalExperimentalRisk) < 0) {
            globalRisk = riskLevel;
          }

          metricValues.add(ExperimentalMetricRecord.ExperimentalMetricAnalysisValue.builder()
                               .name(metricSummary.getMetric_name())
                               .type(metricSummary.getMetric_type())
                               .alertType(metricSummary.getAlert_type())
                               .riskLevel(riskLevel)
                               .experimentalRiskLevel(experimentalRiskLevel)
                               .controlValue(metricSummary.getControl_avg())
                               .testValue(metricSummary.getTest_avg())
                               .build());
        }

        metricAnalysis.add(ExperimentalMetricRecord.ExperimentalMetricAnalysis.builder()
                               .metricName(txnSummary.getTxn_name())
                               .tag(txnSummary.getTxn_tag())
                               .metricValues(metricValues)
                               .riskLevel(globalRisk)
                               .experimentalRiskLevel(globalExperimentalRisk)
                               .build());
      }
    }

    metricRecord.setMetricAnalysis(metricAnalysis);

    if (isNotEmpty(metricAnalysis)) {
      Optional<ExperimentalMetricRecord.ExperimentalMetricAnalysis> highRisk =
          metricAnalysis.stream().filter(analysis -> analysis.getRiskLevel().equals(HIGH)).findAny();
      if (highRisk.isPresent()) {
        metricRecord.setRiskLevel(HIGH);
      } else {
        Optional<ExperimentalMetricRecord.ExperimentalMetricAnalysis> mediumRisk =
            metricAnalysis.stream().filter(analysis -> analysis.getRiskLevel().equals(MEDIUM)).findAny();
        if (mediumRisk.isPresent()) {
          metricRecord.setRiskLevel(MEDIUM);
        } else {
          metricRecord.setRiskLevel(LOW);
        }
      }

      highRisk = metricAnalysis.stream().filter(analysis -> analysis.getExperimentalRiskLevel().equals(HIGH)).findAny();
      if (highRisk.isPresent()) {
        metricRecord.setExperimentalRiskLevel(HIGH);
      } else {
        Optional<ExperimentalMetricRecord.ExperimentalMetricAnalysis> mediumRisk =
            metricAnalysis.stream().filter(analysis -> analysis.getExperimentalRiskLevel().equals(MEDIUM)).findAny();
        if (mediumRisk.isPresent()) {
          metricRecord.setExperimentalRiskLevel(MEDIUM);
        } else {
          metricRecord.setExperimentalRiskLevel(LOW);
        }
      }

    } else {
      metricRecord.setRiskLevel(RiskLevel.NA);
      metricRecord.setExperimentalRiskLevel(RiskLevel.NA);
    }

    if (metricRecord.getStateType() == StateType.DYNA_TRACE && !isEmpty(metricAnalysis)) {
      for (ExperimentalMetricRecord.ExperimentalMetricAnalysis analysis : metricAnalysis) {
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

    return metricRecord;
  }

  @Override
  public LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName) {
    ExperimentalLogMLAnalysisRecord analysisRecord =
        wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class, excludeAuthority)
            .filter(ExperimentalLogMLAnalysisRecordKeys.stateExecutionId, stateExecutionId)
            .order(Sort.descending(ExperimentalLogMLAnalysisRecordKeys.logCollectionMinute))
            .get();
    if (analysisRecord == null) {
      return null;
    }
    LogMLClusterScores logMLClusterScores =
        analysisRecord.getCluster_scores() != null ? analysisRecord.getCluster_scores() : new LogMLClusterScores();
    final LogMLAnalysisSummary analysisSummary =
        LogMLAnalysisSummary.builder()
            .query(analysisRecord.getQuery())
            .score(analysisRecord.getScore() * 100)
            .controlClusters(analysisService.computeCluster(
                analysisRecord.getControl_clusters(), Collections.emptyMap(), AnalysisServiceImpl.CLUSTER_TYPE.CONTROL))
            .testClusters(analysisService.computeCluster(
                analysisRecord.getTest_clusters(), logMLClusterScores.getTest(), AnalysisServiceImpl.CLUSTER_TYPE.TEST))
            .unknownClusters(analysisService.computeCluster(analysisRecord.getUnknown_clusters(),
                logMLClusterScores.getUnknown(), AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN))
            .ignoreClusters(analysisService.computeCluster(
                analysisRecord.getIgnore_clusters(), Collections.emptyMap(), AnalysisServiceImpl.CLUSTER_TYPE.IGNORE))
            .build();

    if (!analysisRecord.isBaseLineCreated()) {
      analysisSummary.setTestClusters(analysisSummary.getControlClusters());
      analysisSummary.setControlClusters(new ArrayList<>());
    }

    RiskLevel riskLevel = RiskLevel.NA;
    String analysisSummaryMsg = isEmpty(analysisRecord.getAnalysisSummaryMessage())
        ? analysisSummary.getControlClusters().isEmpty()
            ? "No baseline data for the given queries. This will be baseline for the next run."
            : analysisSummary.getTestClusters().isEmpty()
                ? "No new data for the given queries. Showing baseline data if any."
                : "No anomaly found"
        : analysisRecord.getAnalysisSummaryMessage();

    int unknownClusters = 0;
    int highRiskClusters = 0;
    int mediumRiskCluster = 0;
    int lowRiskClusters = 0;
    if (isNotEmpty(analysisSummary.getUnknownClusters())) {
      for (LogMLClusterSummary clusterSummary : analysisSummary.getUnknownClusters()) {
        if (clusterSummary.getScore() > HIGH_RISK_THRESHOLD) {
          ++highRiskClusters;
        } else if (clusterSummary.getScore() > MEDIUM_RISK_THRESHOLD) {
          ++mediumRiskCluster;
        } else if (clusterSummary.getScore() > 0) {
          ++lowRiskClusters;
        }
      }
      riskLevel = highRiskClusters > 0
          ? RiskLevel.HIGH
          : mediumRiskCluster > 0 ? RiskLevel.MEDIUM : lowRiskClusters > 0 ? RiskLevel.LOW : RiskLevel.HIGH;

      unknownClusters = analysisSummary.getUnknownClusters().size();
      analysisSummary.setHighRiskClusters(highRiskClusters);
      analysisSummary.setMediumRiskClusters(mediumRiskCluster);
      analysisSummary.setLowRiskClusters(lowRiskClusters);
    }

    int unknownFrequency = analysisService.getUnexpectedFrequency(analysisRecord.getTest_clusters());
    if (unknownFrequency > 0) {
      analysisSummary.setHighRiskClusters(analysisSummary.getHighRiskClusters() + unknownFrequency);
      riskLevel = RiskLevel.HIGH;
    }

    if (highRiskClusters > 0 || mediumRiskCluster > 0 || lowRiskClusters > 0) {
      analysisSummaryMsg = analysisSummary.getHighRiskClusters() + " high risk, "
          + analysisSummary.getMediumRiskClusters() + " medium risk, " + analysisSummary.getLowRiskClusters()
          + " low risk anomalous cluster(s) found";
    } else if (unknownClusters > 0 || unknownFrequency > 0) {
      final int totalAnomalies = unknownClusters + unknownFrequency;
      analysisSummaryMsg = totalAnomalies == 1 ? totalAnomalies + " anomalous cluster found"
                                               : totalAnomalies + " anomalous clusters found";
    }

    analysisSummary.setRiskLevel(riskLevel);
    analysisSummary.setAnalysisSummaryMessage(analysisSummaryMsg);
    return analysisSummary;
  }

  @Override
  public List<ExperimentalMessageComparisonResult> getMessagePairsToVote(String serviceId) {
    logger.info("Getting msg pairs to vote {}", serviceId);
    List<ExperimentalMessageComparisonResult> messagesToShow = new ArrayList<>();
    User currentUser = UserThreadLocal.get();

    try (HIterator<CVConfiguration> cvConfigurationHIterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority)
                                 .filter(CVConfigurationKeys.serviceId, serviceId)
                                 .fetch())) {
      while (cvConfigurationHIterator.hasNext() && messagesToShow.size() <= 10) {
        CVConfiguration cvConfiguration = cvConfigurationHIterator.next();
        PageRequest<ExperimentalMessageComparisonResult> comparisonResultPageRequest =
            PageRequestBuilder.aPageRequest()
                .addFilter(ExperimentalMessageComparisonResultKeys.cvConfigId, Operator.EQ, cvConfiguration.getUuid())
                .addFilter(ExperimentalMessageComparisonResultKeys.numVotes, Operator.LT, 3)
                .build();
        logger.info("Querying GDS for serviceID {}, cvConfigId {}", serviceId, cvConfiguration.getUuid());
        List<ExperimentalMessageComparisonResult> comparisonResults =
            dataStoreService.list(ExperimentalMessageComparisonResult.class, comparisonResultPageRequest);
        logger.info("Got {} comparison results from GDS for msg pairs to vote. serviceID {}, cvConfigId {}",
            comparisonResults.size(), serviceId, cvConfiguration.getUuid());
        if (isNotEmpty(comparisonResults)) {
          for (ExperimentalMessageComparisonResult result : comparisonResults) {
            if (messagesToShow.size() >= 10) {
              break;
            }

            if (isEmpty(result.getUserVotes())) {
              messagesToShow.add(result);
              continue;
            }

            if (!result.getUserVotes().containsKey(currentUser.getName())) {
              messagesToShow.add(result);
              continue;
            }
          }
        }
      }
    }
    logger.info("Returning {} msg pairs to vote. serviceID {}", messagesToShow.size(), serviceId);
    return messagesToShow;
  }

  @Override
  public boolean saveMessagePairsToVote(String serviceId, Map<String, String> userVotes) {
    User currentUser = UserThreadLocal.get();
    if (isNotEmpty(userVotes)) {
      List<ExperimentalMessageComparisonResult> resultsToSave = new ArrayList<>();
      userVotes.forEach((uuid, vote) -> {
        ExperimentalMessageComparisonResult result =
            dataStoreService.getEntity(ExperimentalMessageComparisonResult.class, uuid);
        if (result.getUserVotes() == null) {
          result.setUserVotes(new HashMap<>());
        }
        result.getUserVotes().put(currentUser.getName(), vote);
        result.incrementNumVotes();
        resultsToSave.add(result);
      });
      dataStoreService.save(ExperimentalMessageComparisonResult.class, resultsToSave, false);
    }
    return true;
  }
}
