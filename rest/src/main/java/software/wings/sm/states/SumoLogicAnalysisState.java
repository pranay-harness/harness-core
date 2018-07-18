package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.time.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.impl.sumo.SumoSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public class SumoLogicAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(SumoLogicAnalysisState.class);

  @Attributes(required = true, title = "Sumo Logic Server") protected String analysisServerConfigId;

  public SumoLogicAnalysisState(String name) {
    super(name, StateType.SUMO.getType());
  }

  @Override
  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  @EnumData(enumDataProvider = SumoSettingProvider.class)
  @Attributes(required = true, title = "Sumo Logic Server")
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = null;
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        settingAttribute = templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
      }
    }
    if (settingAttribute == null) {
      settingAttribute = settingsService.get(analysisServerConfigId);
      if (settingAttribute == null) {
        throw new WingsException("No sumo setting with id: " + analysisServerConfigId + " found");
      }
    }

    final SumoConfig sumoConfig = (SumoConfig) settingAttribute.getValue();
    final long logCollectionStartTimeStamp = Timestamp.currentMinuteBoundary();

    List<Set<String>> batchedHosts = batchHosts(hosts);
    String[] waitIds = new String[batchedHosts.size()];
    List<DelegateTask> delegateTasks = new ArrayList<>();
    int i = 0;
    for (Set<String> hostBatch : batchedHosts) {
      final SumoDataCollectionInfo dataCollectionInfo =
          SumoDataCollectionInfo.builder()
              .sumoConfig(sumoConfig)
              .accountId(appService.get(context.getAppId()).getAccountId())
              .applicationId(context.getAppId())
              .stateExecutionId(context.getStateExecutionInstanceId())
              .workflowId(getWorkflowId(context))
              .workflowExecutionId(context.getWorkflowExecutionId())
              .serviceId(getPhaseServiceId(context))
              .query(query)
              .startTime(logCollectionStartTimeStamp)
              .startMinute(0)
              .collectionTime(Integer.parseInt(timeDuration))
              .hosts(hostBatch)
              .encryptedDataDetails(
                  secretManager.getEncryptionDetails(sumoConfig, context.getAppId(), context.getWorkflowExecutionId()))
              .hostnameField(getHostnameField())
              .build();

      String waitId = generateUuid();
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
      delegateTasks.add(aDelegateTask()
                            .withTaskType(TaskType.SUMO_COLLECT_LOG_DATA)
                            .withAccountId(appService.get(context.getAppId()).getAccountId())
                            .withAppId(context.getAppId())
                            .withWaitId(waitId)
                            .withParameters(new Object[] {dataCollectionInfo})
                            .withEnvId(envId)
                            .withInfrastructureMappingId(infrastructureMappingId)
                            .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 60))
                            .build());
      waitIds[i++] = waitId;
    }
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId, true), waitIds);
    List<String> delegateTaskIds = new ArrayList<>();
    for (DelegateTask task : delegateTasks) {
      delegateTaskIds.add(delegateService.queueTask(task));
    }
    return StringUtils.join(delegateTaskIds, ",");
  }

  @DefaultValue("_sourceHost")
  @Attributes(required = true, title = "Field name for Host/Container")
  public String getHostnameField() {
    if (isEmpty(hostnameField)) {
      return "_sourceHost";
    }
    return hostnameField;
  }

  public void setHostnameField(String hostnameField) {
    this.hostnameField = hostnameField;
  }

  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }
}
