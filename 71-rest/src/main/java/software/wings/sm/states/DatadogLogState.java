package software.wings.sm.states;

import static software.wings.common.VerificationConstants.DATA_DOG_DEFAULT_HOSTNAME;
import static software.wings.common.VerificationConstants.KUBERNETES_HOSTNAME;
import static software.wings.utils.Misc.replaceDotWithUnicode;

import com.google.common.base.Preconditions;

import com.github.reinert.jjschema.Attributes;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.api.DeploymentType;
import software.wings.beans.DatadogConfig;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class DatadogLogState extends AbstractLogAnalysisState {
  public static final String HOST_NAME_SEPARATOR = " OR ";

  protected String analysisServerConfigId;

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

  public void setHostnameField(String hostnameField) {
    this.hostnameField = hostnameField;
  }

  public String getHostnameField() {
    return hostnameField;
  }

  @Attributes(required = false, title = "Expression for Host/Container name")
  @Override
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  @Override
  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  @Override
  protected String getHostnameField(ExecutionContext executionContext) {
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

    List<String> pathList3 = Collections.singletonList("logs[*].content.tags.[*]");
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .regexs(Collections.singletonList("((?<=" + hostnameField + ":)(.*))"))
            .jsonPath(pathList3)
            .build());

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
