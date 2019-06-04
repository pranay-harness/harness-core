package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;
import static software.wings.sm.states.SumoLogicAnalysisState.SumoHostNameField.SOURCE_HOST;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import software.wings.api.PhaseElement;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
@Slf4j
public class SumoLogicAnalysisState extends AbstractLogAnalysisState {
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
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts) {
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
              .query(getRenderedQuery())
              .startTime(logCollectionStartTimeStamp)
              .startMinute((int) (logCollectionStartTimeStamp / TimeUnit.MINUTES.toMillis(1)))
              .collectionTime(Integer.parseInt(getTimeDuration()))
              .hosts(hostBatch)
              .encryptedDataDetails(
                  secretManager.getEncryptionDetails(sumoConfig, context.getAppId(), context.getWorkflowExecutionId()))
              .hostnameField(getHostnameField().getHostNameField())
              .initialDelayMinutes(DELAY_MINUTES)
              .build();

      String waitId = generateUuid();
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
      delegateTasks.add(DelegateTask.builder()
                            .async(true)
                            .accountId(appService.get(context.getAppId()).getAccountId())
                            .appId(context.getAppId())
                            .waitId(waitId)
                            .data(TaskData.builder()
                                      .taskType(TaskType.SUMO_COLLECT_LOG_DATA.name())
                                      .parameters(new Object[] {dataCollectionInfo})
                                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 60))
                                      .build())
                            .envId(envId)
                            .infrastructureMappingId(infrastructureMappingId)
                            .build());
      waitIds[i++] = waitId;
    }
    waitNotifyEngine.waitForAll(
        DataCollectionCallback.builder().appId(context.getAppId()).executionData(executionData).build(), waitIds);
    List<String> delegateTaskIds = new ArrayList<>();
    for (DelegateTask task : delegateTasks) {
      delegateTaskIds.add(delegateService.queueTask(task));
    }
    return StringUtils.join(delegateTaskIds, ",");
  }

  @DefaultValue("_sourceHost")
  @Attributes(required = true, title = "Field name for Host/Container")
  public SumoHostNameField getHostnameField() {
    if (isEmpty(hostnameField)) {
      return SOURCE_HOST;
    }
    return SumoHostNameField.getHostNameFieldFromValue(hostnameField);
  }

  public void setHostnameField(String hostnameField) {
    SumoHostNameField.getHostNameFieldFromValue(hostnameField);
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

  public enum SumoHostNameField {
    SOURCE_HOST("_sourceHost"),
    SOURCE_NAME("_sourceName");

    private String hostNameField;

    SumoHostNameField(String hostNameField) {
      this.hostNameField = hostNameField;
    }

    public String getHostNameField() {
      return hostNameField;
    }

    public static SumoHostNameField getHostNameFieldFromValue(String hostNameField) {
      for (SumoHostNameField sumoHost : SumoHostNameField.values()) {
        if (sumoHost.getHostNameField().equalsIgnoreCase(hostNameField.trim())) {
          return sumoHost;
        }
      }
      throw new WingsException("Invalid host name field " + hostNameField, WingsException.USER);
    }
  }
}
