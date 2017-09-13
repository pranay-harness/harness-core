package software.wings.service.impl.newrelic;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicServiceImpl implements NewRelicService {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public void validateConfig(SettingAttribute settingAttribute) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .validateConfig((NewRelicConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.NEWRELIC_CONFIGURATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public List<NewRelicApplication> getApplications(String settingId) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .getAllApplications((NewRelicConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(
          ErrorCode.NEWRELIC_ERROR, "message", "Error in getting new relic applications. " + e.getMessage());
    }
  }

  @Override
  public boolean saveMetricData(String accountId, String applicationId, List<NewRelicMetricDataRecord> metricData)
      throws IOException {
    logger.debug("inserting " + metricData.size() + " pieces of new relic metrics data");
    wingsPersistence.saveIgnoringDuplicateKeys(metricData);
    logger.debug("inserted " + metricData.size() + " NewRelicMetricDataRecord to persistence layer.");
    return true;
  }

  @Override
  public boolean saveAnalysisRecords(NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                .field("workflowExecutionId")
                                .equal(metricAnalysisRecord.getWorkflowExecutionId())
                                .field("stateExecutionId")
                                .equal(metricAnalysisRecord.getStateExecutionId()));

    wingsPersistence.save(metricAnalysisRecord);
    logger.debug("inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: "
        + metricAnalysisRecord.getWorkflowExecutionId()
        + " StateExecutionInstanceId: " + metricAnalysisRecord.getStateExecutionId());
    return true;
  }

  @Override
  public List<NewRelicMetricDataRecord> getRecords(String workflowExecutionId, String stateExecutionId,
      String workflowId, String serviceId, Set<String> nodes, int analysisMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
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
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);
    return query.asList();
  }

  @Override
  public List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      String workflowId, String serviceId, int analysisMinute) {
    final String astSuccessfulWorkflowExecutionIdWithData =
        getLastSuccessfulWorkflowExecutionIdWithData(workflowId, serviceId);
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("workflowId")
                                                .equal(workflowId)
                                                .field("workflowExecutionId")
                                                .equal(astSuccessfulWorkflowExecutionIdWithData)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);
    return query.asList();
  }

  private String getLastSuccessfulWorkflowExecutionIdWithData(String workflowId, String serviceId) {
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(workflowId);
    for (String successfulExecution : successfulExecutions) {
      Query<NewRelicMetricDataRecord> lastSuccessfulRecordQuery =
          wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
              .field("workflowId")
              .equal(workflowId)
              .field("workflowExecutionId")
              .equal(successfulExecution)
              .field("serviceId")
              .equal(serviceId)
              .limit(1);

      List<NewRelicMetricDataRecord> lastSuccessfulRecords = lastSuccessfulRecordQuery.asList();
      if (lastSuccessfulRecords != null && lastSuccessfulRecords.size() > 0) {
        return successfulExecution;
      }
    }
    logger.error("Could not get a successful workflow to find control nodes");
    return null;
  }

  private List<String> getLastSuccessfulWorkflowExecutionIds(String workflowId) {
    final PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest()
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

  @Override
  public NewRelicMetricAnalysisRecord getMetricsAnalysis(String stateExecutionId, String workflowExecutionId) {
    Query<NewRelicMetricAnalysisRecord> splunkLogMLAnalysisRecords =
        wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("workflowExecutionId")
            .equal(workflowExecutionId);
    NewRelicMetricAnalysisRecord analysisRecord = wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
    if (analysisRecord == null) {
      return null;
    }

    if (analysisRecord.getMetricAnalyses() == null) {
      return NewRelicMetricAnalysisRecord.builder()
          .message(
              "Could not get metric data from new relic. Please make sure that the new relic account is a paid account and metrics can be pulled using rest API")
          .build();
    }

    int highRisk = 0;
    int mediumRisk = 0;
    for (NewRelicMetricAnalysis metricAnalysis : analysisRecord.getMetricAnalyses()) {
      switch (metricAnalysis.getRiskLevel()) {
        case HIGH:
          highRisk++;
          break;
        case MEDIUM:
          mediumRisk++;
          break;
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

    Collections.sort(analysisRecord.getMetricAnalyses());
    return analysisRecord;
  }

  @Override
  public boolean isStateValid(String appdId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appdId, stateExecutionID);
    return (stateExecutionInstance == null || stateExecutionInstance.getStatus().isFinalStatus()) ? false : true;
  }

  @Override
  public int getCollectionMinuteToProcess(String stateExecutionId, String workflowExecutionId, String serviceId) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionId)
                                                .field("stateExecutionId")
                                                .equal(stateExecutionId)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .equal(ClusterLevel.HF)
                                                .order("-dataCollectionMinute")
                                                .limit(1);

    if (query.asList().size() == 0) {
      logger.info(
          "No metric record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}. Will be running analysis for minute 0",
          ClusterLevel.HF, stateExecutionId, workflowExecutionId, serviceId);
      return 0;
    }

    return query.asList().get(0).getDataCollectionMinute() + 1;
  }

  @Override
  public void bumpCollectionMinuteToProcess(
      String stateExecutionId, String workflowExecutionId, String serviceId, int analysisMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
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

    wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(NewRelicMetricDataRecord.class).set("level", ClusterLevel.HF));
  }
}
