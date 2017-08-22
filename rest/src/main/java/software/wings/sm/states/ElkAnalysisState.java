package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.collect.Sets;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.LogCollectionCallback;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by raghu on 8/4/17.
 */
public class ElkAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(ElkAnalysisState.class);

  @Attributes(required = true, title = "Elastic Search Server") protected String analysisServerConfigId;

  protected String indices;

  @Attributes(
      title = "Elastic search indices to search", description = "Comma separated list of indices : _index1,_index2")
  @DefaultValue("_all")
  public String
  getIndices() {
    return indices;
  }

  public void setIndices(String indices) {
    this.indices = indices;
  }

  public ElkAnalysisState(String name) {
    super(name, StateType.ELK.getType());
  }

  public ElkAnalysisState(String name, String type) {
    super(name, type);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final ElkDataCollectionInfo dataCollectionInfo = new ElkDataCollectionInfo(elkConfig,
        appService.get(context.getAppId()).getAccountId(), context.getAppId(), context.getStateExecutionInstanceId(),
        getWorkflowId(context), context.getWorkflowExecutionId(), getPhaseServiceId(context), queries, indices,
        logCollectionStartTimeStamp, Integer.parseInt(timeDuration), hosts);
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.ELK_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .build();
    waitNotifyEngine.waitForAll(new LogCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }

  @Override
  @EnumData(enumDataProvider = ElkSettingProvider.class)
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected void preProcess(ExecutionContext context, int logAnalysisMinute) {
    Set<String> testNodes = getCanaryNewHostNames(context);
    Set<String> controlNodes = getLastExecutionNodes(context);
    Set<String> allNodes = new HashSet<>();

    if (controlNodes != null) {
      allNodes.addAll(controlNodes);
    }

    if (testNodes != null) {
      allNodes.addAll(testNodes);
    }

    final String accountId = appService.get(context.getAppId()).getAccountId();
    String serviceId = getPhaseServiceId(context);
    LogRequest logRequest = new LogRequest(query, context.getAppId(), context.getStateExecutionInstanceId(),
        getWorkflowId(context), serviceId, allNodes, logAnalysisMinute);
    analysisService.finalizeLogCollection(accountId, StateType.ELK, context.getWorkflowExecutionId(), logRequest);
  }
}
