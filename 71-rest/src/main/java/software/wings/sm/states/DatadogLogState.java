package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.DATA_DOG_DEFAULT_HOSTNAME;
import static software.wings.common.VerificationConstants.KUBERNETES_HOSTNAME;
import static software.wings.utils.Misc.replaceDotWithUnicode;

import com.google.common.base.Preconditions;

import com.github.reinert.jjschema.Attributes;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.api.DeploymentType;
import software.wings.beans.DatadogConfig;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class DatadogLogState extends AbstractLogAnalysisState {
  public static final String HOST_NAME_SEPARATOR = " OR ";

  @Attributes(required = true, title = "Datadog Log Server") protected String analysisServerConfigId;

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
  public Logger getLogger() {
    return logger;
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  @Override
  public String getQuery() {
    return query;
  }

  @Override
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

  public DatadogLogState(String name) {
    super(name, StateType.DATA_DOG_LOG.name());
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts) {
    return null;
  }

  @Override
  public void setQuery(String query) {
    this.query = query;
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  @Override
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
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
  public String getHostnameField(ExecutionContext executionContext) {
    if (hostnameField != null) {
      return hostnameField;
    }
    if (getDeploymentType(executionContext) == DeploymentType.KUBERNETES) {
      return KUBERNETES_HOSTNAME;
    } else {
      return DATA_DOG_DEFAULT_HOSTNAME;
    }
  }

  public static Map<String, Map<String, CustomLogVerificationState.ResponseMapper>> constructLogDefinitions(
      DatadogConfig datadogConfig, String hostnameField, boolean is24x7) {
    Map<String, Map<String, CustomLogVerificationState.ResponseMapper>> logDefinition = new HashMap<>();
    Map<String, CustomLogVerificationState.ResponseMapper> responseMappers = new HashMap<>();
    List<String> pathList = Collections.singletonList("logs[*].content.timestamp");
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(pathList)
            .timestampFormat("")
            .build());
    List<String> pathList2 = Collections.singletonList("logs[*].content.message");
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder().fieldName("logMessage").jsonPath(pathList2).build());

    if (!is24x7) {
      List<String> pathList3 = Collections.singletonList("logs[*].content.tags[*]." + hostnameField);
      responseMappers.put(
          "host", CustomLogVerificationState.ResponseMapper.builder().fieldName("host").jsonPath(pathList3).build());
    }

    String eventsUrl = datadogConfig.getUrl() + DatadogConfig.LOG_API_PATH_SUFFIX;
    logDefinition.put(replaceDotWithUnicode(eventsUrl), responseMappers);
    return logDefinition;
  }

  public static Map<String, Object> resolveHostnameField(Map<String, Object> body, String hostnameField) {
    String queryKey = "query";
    String queryString = (String) body.get(queryKey);
    Preconditions.checkNotNull(queryString, "query string can not be null");
    queryString = queryString.replace("${hostname_field}", hostnameField);
    body.put(queryKey, queryString);
    return body;
  }
}
