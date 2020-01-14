package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.TemplateExpression;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Set;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
@Slf4j
public class SplunkV2State extends AbstractLogAnalysisState {
  @Attributes(required = true, title = "Splunk Server") private String analysisServerConfigId;

  private boolean isAdvancedQuery;

  public SplunkV2State(String name) {
    super(name, StateType.SPLUNKV2.getType());
  }

  @Override
  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
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

  @Override
  @Attributes(title = "Analysis Time duration (in minutes)")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  @DefaultValue("host")
  @Attributes(required = true, title = "Field name for Host/Container")
  public String getHostnameField() {
    if (isEmpty(hostnameField)) {
      return "host";
    }
    return hostnameField;
  }

  public void setHostnameField(String hostnameField) {
    this.hostnameField = hostnameField;
  }

  @Override
  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  @Override
  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  @DefaultValue("false")
  @Attributes(required = false, title = "Is this an advanced splunk query")
  public boolean isAdvancedQuery() {
    return isAdvancedQuery;
  }

  public void setIsAdvancedQuery(boolean advancedQuery) {
    this.isAdvancedQuery = advancedQuery;
  }

  private String getServerConfigId(ExecutionContext context) {
    String finalServerConfigId = analysisServerConfigId;
    if (isNotEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        finalServerConfigId = templateExpressionProcessor.resolveTemplateExpression(context, configIdExpression);
      }
    }
    return finalServerConfigId;
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts) {
    throw new UnsupportedOperationException(
        "This should not get called. Splunk is now using new data collection framework");
  }
  @Override
  public DataCollectionInfoV2 createDataCollectionInfo(ExecutionContext context, Set<String> hosts) {
    // TODO: see if this can be moved to base class.
    // TODO: some common part needs to be refactored

    String finalServerConfigId = getServerConfigId(context);
    String envId = getEnvId(context);
    return SplunkDataCollectionInfoV2.builder()
        .connectorId(finalServerConfigId)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .stateExecutionId(context.getStateExecutionInstanceId())
        .workflowId(context.getWorkflowId())
        .accountId(appService.get(context.getAppId()).getAccountId())
        .envId(envId)
        .applicationId(context.getAppId())
        .query(getRenderedQuery())
        .hostnameField(getHostnameField())
        .hosts(hosts)
        .isAdvancedQuery(isAdvancedQuery)
        .build();
  }

  @Override
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

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  @Override
  protected boolean isCVTaskEnqueuingEnabled(String accountId) {
    return true;
  }
}
