package software.wings.service.impl.splunk;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.splunk.SplunkService;

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
public class SplunkServiceImpl implements SplunkService {
  private static final Logger logger = LoggerFactory.getLogger(SplunkServiceImpl.class);
  private final Random random = new Random();

  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public Boolean saveLogData(String appId, String stateExecutionId, List<SplunkLogElement> logData) throws IOException {
    logger.debug("inserting " + logData.size() + " pieces of splunk log data");
    final List<SplunkLogDataRecord> logDataRecords =
        SplunkLogDataRecord.generateDataRecords(appId, stateExecutionId, logData);
    wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
    logger.debug("inserted " + logDataRecords.size() + " SplunkLogDataRecord to persistence layer.");
    return true;
  }

  @Override
  public List<SplunkLogDataRecord> getSplunkLogData(SplunkLogRequest logRequest) {
    Query<SplunkLogDataRecord> splunkLogDataRecordQuery = wingsPersistence.createQuery(SplunkLogDataRecord.class)
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

    List<SplunkLogDataRecord> records = splunkLogDataRecordQuery.asList();
    logger.debug("returning " + records.size() + " records for request: " + logRequest);
    return records;
  }

  @Override
  public Boolean markProcessed(String stateExecutionId, String applicationId, long tillTimeStamp) {
    Query<SplunkLogDataRecord> splunkLogDataRecords = wingsPersistence.createQuery(SplunkLogDataRecord.class)
                                                          .field("stateExecutionId")
                                                          .equal(stateExecutionId)
                                                          .field("applicationId")
                                                          .equal("applicationId")
                                                          .field("timeStamp")
                                                          .lessThanOrEq(tillTimeStamp);

    wingsPersistence.update(splunkLogDataRecords,
        wingsPersistence.createUpdateOperations(SplunkLogDataRecord.class).set("processed", true));
    return true;
  }

  @Override
  public boolean isLogDataCollected(
      String applicationId, String stateExecutionId, String query, int logCollectionMinute) {
    Query<SplunkLogDataRecord> splunkLogDataRecordQuery = wingsPersistence.createQuery(SplunkLogDataRecord.class)
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

  @Override
  public Boolean saveSplunkAnalysisRecords(SplunkLogMLAnalysisRecord mlAnalysisResponse) {
    wingsPersistence.delete(wingsPersistence.createQuery(SplunkLogMLAnalysisRecord.class)
                                .field("applicationId")
                                .equal(mlAnalysisResponse.getApplicationId())
                                .field("stateExecutionId")
                                .equal(mlAnalysisResponse.getStateExecutionId()));

    wingsPersistence.save(mlAnalysisResponse);
    logger.debug(
        "inserted ml SplunkLogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getApplicationId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    return true;
  }

  @Override
  public SplunkLogMLAnalysisRecord getSplunkAnalysisRecords(
      String applicationId, String stateExecutionId, String query) {
    Query<SplunkLogMLAnalysisRecord> splunkLogMLAnalysisRecords =
        wingsPersistence.createQuery(SplunkLogMLAnalysisRecord.class)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("applicationId")
            .equal(applicationId)
            .field("query")
            .equal(query);
    return wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
  }

  @Override
  public SplunkMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId) {
    Query<SplunkLogMLAnalysisRecord> splunkLogMLAnalysisRecords =
        wingsPersistence.createQuery(SplunkLogMLAnalysisRecord.class)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("applicationId")
            .equal(applicationId);
    SplunkLogMLAnalysisRecord analysisRecord = wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
    final SplunkMLAnalysisSummary analysisSummary = new SplunkMLAnalysisSummary();

    if (analysisRecord == null) {
      return analysisSummary;
    }

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
      analysisSummaryMsg = (unknownClusters + unknownFrequency) + " anomalous events found";
    }

    analysisSummary.setRiskLevel(riskLevel);
    analysisSummary.setAnalysisSummaryMessage(analysisSummaryMsg);
    return analysisSummary;
  }

  @Override
  public void validateConfig(final SettingAttribute settingAttribute) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(SplunkDelegateService.class, syncTaskContext)
          .validateConfig((SplunkConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.SPLUNK_CONFIGURATION_ERROR, "reason", e.getMessage());
    }
  }

  private List<SplunkMLClusterSummary> computeCluster(Map<String, Map<String, SplunkAnalysisCluster>> cluster) {
    if (cluster == null) {
      return Collections.emptyList();
    }
    final List<SplunkMLClusterSummary> analysisSummaries = new ArrayList<>();
    for (Entry<String, Map<String, SplunkAnalysisCluster>> labelEntry : cluster.entrySet()) {
      final SplunkMLClusterSummary clusterSummary = new SplunkMLClusterSummary();
      clusterSummary.setHostSummary(new HashMap<>());
      for (Entry<String, SplunkAnalysisCluster> hostEntry : labelEntry.getValue().entrySet()) {
        final SplunkMLHostSummary hostSummary = new SplunkMLHostSummary();
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

  private int getUnexpectedFrequency(SplunkMLAnalysisSummary analysisSummary) {
    int unexpectedFrequency = 0;
    if (analysisSummary.getTestClusters() == null) {
      return unexpectedFrequency;
    }

    for (SplunkMLClusterSummary clusterSummary : analysisSummary.getTestClusters()) {
      for (Entry<String, SplunkMLHostSummary> hostEntry : clusterSummary.getHostSummary().entrySet()) {
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
