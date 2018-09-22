package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BugsnagState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(BugsnagState.class);
  @SchemaIgnore
  @Transient
  private static final String FETCH_EVENTS_URL =
      "projects/:projectId:/events?filters[event.since][][value]=${iso_start_time}&filters[event.since][][type]=eq&full_reports=true&per_page=1000";
  @SchemaIgnore @Transient @Inject private EncryptionService encryptionService;

  public BugsnagState(String name) {
    super(name, StateType.BUG_SNAG.getType());
  }

  public BugsnagState(String name, String type) {
    super(name, type);
  }

  @Attributes(required = true, title = "Bugsnag Server") protected String analysisServerConfigId;

  @Attributes(required = true, title = "Bugsnag Organization") protected String orgId;

  @Attributes(required = true, title = "Bugsnag Project") protected String projectId;

  @Attributes(required = true, title = "Bugsnag Project") protected boolean browserApplication;

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  @Override
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Override
  @SchemaIgnore
  public String getQuery() {
    return query;
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  public String getProjectId() {
    return orgId;
  }

  public boolean isBrowserApplication() {
    return browserApplication;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setBrowserApplication(Boolean browserApplication) {
    this.browserApplication = browserApplication;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
    this.query = "";
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  @Attributes(required = true, title = "Include nodes from previous phases")
  public boolean getIncludePreviousPhaseNodes() {
    return includePreviousPhaseNodes;
  }

  @Attributes(title = "Analysis Time duration (in minutes)")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, LogAnalysisExecutionData executionData, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = null;
    String serverConfigId = analysisServerConfigId;
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        settingAttribute = templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        serverConfigId = settingAttribute.getUuid();
      }
    }
    if (settingAttribute == null) {
      settingAttribute = settingsService.get(serverConfigId);
      if (settingAttribute == null) {
        throw new WingsException("No bugsnag setting with id: " + analysisServerConfigId + " found");
      }
    }

    final BugsnagConfig config = (BugsnagConfig) settingAttribute.getValue();
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    String accountId = appService.get(context.getAppId()).getAccountId();

    // Form the dataCollectionInfo
    CustomLogDataCollectionInfo dataCollectionInfo =
        CustomLogDataCollectionInfo.builder()
            .baseUrl(config.getUrl())
            .validationUrl(BugsnagConfig.validationUrl)
            .headers(config.headersMap())
            .options(config.optionsMap())
            .query("")
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(config, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .stateType(StateType.BUG_SNAG)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .startMinute(0)
            .responseDefinition(constructLogDefinitions(context))
            .shouldInspectHosts(!isBrowserApplication())
            .collectionFrequency(1)
            .collectionTime(Integer.parseInt(timeDuration))
            .accountId(accountId)
            .build();

    // Create the delegate task and send it over.

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.CUSTOM_LOG_COLLECTION_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), executionData, false), waitId);
    return delegateService.queueTask(delegateTask);
  }

  protected Map<String, Map<String, ResponseMapper>> constructLogDefinitions(final ExecutionContext context) {
    Map<String, Map<String, ResponseMapper>> logDefinition = new HashMap<>();
    if (isEmpty(projectId)) {
      throw new WingsException("ProjectID is empty in Bugsnag State. Unable to fetch data");
    }
    String eventsUrl = FETCH_EVENTS_URL.replace(":projectId:", projectId);
    logDefinition.put(eventsUrl, new HashMap<>());
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    List<String> pathList = new ArrayList<>();
    pathList.add("[*].received_at");
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(pathList)
            .timestampFormat("")
            .build());
    List<String> pathList2 = new ArrayList<>();
    pathList2.add("[*].context");
    pathList2.add("[*].request");
    pathList2.add("[*].metaData");
    pathList2.add("[*].exceptions[0]");
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder().fieldName("logMessage").jsonPath(pathList2).build());
    List<String> pathList3 = new ArrayList<>();
    pathList3.add("[*].device.browserName");
    responseMappers.put(
        "host", CustomLogVerificationState.ResponseMapper.builder().fieldName("host").jsonPath(pathList3).build());
    logDefinition.put(eventsUrl, responseMappers);
    return logDefinition;
  }
}
