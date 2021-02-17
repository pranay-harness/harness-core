package io.harness.delegate.beans.connector.jira;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JiraCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, JiraConnectorDTO jiraConnectorDTO) {
    String jiraUrl = jiraConnectorDTO.getJiraUrl();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        jiraUrl.endsWith("/") ? jiraUrl : jiraUrl.concat("/"), maskingEvaluator));
  }
}
