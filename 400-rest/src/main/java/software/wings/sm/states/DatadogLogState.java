/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.logging.Misc.replaceDotWithUnicode;

import static software.wings.common.VerificationConstants.DATA_DOG_DEFAULT_HOSTNAME;
import static software.wings.common.VerificationConstants.KUBERNETES_HOSTNAME;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.DeploymentType;
import software.wings.beans.DatadogConfig;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;
import software.wings.stencils.DefaultValue;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import com.github.reinert.jjschema.Attributes;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class DatadogLogState extends AbstractLogAnalysisState {
  public static final String HOST_NAME_SEPARATOR = " OR ";
  public static final String HOST_NAME_RESERVED_FIELD = "host";

  protected String analysisServerConfigId;

  @Override
  public Logger getLogger() {
    return log;
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

  public static Map<String, Map<String, ResponseMapper>> constructLogDefinitions(
      DatadogConfig datadogConfig, String hostnameField, boolean is24x7) {
    Map<String, Map<String, ResponseMapper>> logDefinition = new HashMap<>();
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    List<String> pathList = Collections.singletonList("logs[*].content.timestamp");
    responseMappers.put(
        "timestamp", ResponseMapper.builder().fieldName("timestamp").jsonPath(pathList).timestampFormat("").build());
    List<String> pathList2 = Collections.singletonList("logs[*].content.message");
    responseMappers.put("logMessage", ResponseMapper.builder().fieldName("logMessage").jsonPath(pathList2).build());

    if (HOST_NAME_RESERVED_FIELD.equals(hostnameField)) {
      List<String> pathList3 = Collections.singletonList("logs[*].content." + hostnameField);
      responseMappers.put("host", ResponseMapper.builder().fieldName("host").jsonPath(pathList3).build());
    } else {
      List<String> pathList3 = Collections.singletonList("logs[*].content.tags.[*]");
      responseMappers.put("host",
          ResponseMapper.builder()
              .fieldName("host")
              .regexs(Collections.singletonList("((?<=" + hostnameField + ":)(.*))"))
              .jsonPath(pathList3)
              .build());
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
