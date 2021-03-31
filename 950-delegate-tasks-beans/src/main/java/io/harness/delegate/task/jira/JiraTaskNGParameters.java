package io.harness.delegate.task.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.JiraCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.jira.JiraActionNG;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JiraTaskNGParameters implements TaskParameters, ExecutionCapabilityDemander {
  JiraConnectorDTO jiraConnectorDTO;
  List<EncryptedDataDetail> encryptionDetails;

  JiraActionNG action;
  String projectKey;
  String issueType;
  String issueKey;

  // Jira apis have an expand query param to fetch more information. Look at JiraClient apis to know more.
  String expand;
  // Fetch status along with create metadata.
  boolean fetchStatus;
  // Fields sent while creating/updating issue.
  Map<String, String> fields;

  public Set<String> getDelegateSelectors() {
    if (jiraConnectorDTO == null || jiraConnectorDTO.getDelegateSelectors() == null) {
      return Collections.emptySet();
    }
    return jiraConnectorDTO.getDelegateSelectors();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return JiraCapabilityGenerator.generateDelegateCapabilities(jiraConnectorDTO, encryptionDetails, maskingEvaluator);
  }
}
