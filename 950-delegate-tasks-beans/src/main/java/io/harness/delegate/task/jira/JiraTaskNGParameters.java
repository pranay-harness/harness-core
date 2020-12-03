package io.harness.delegate.task.jira;

import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.JiraCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JiraTaskNGParameters implements TaskParameters, ExecutionCapabilityDemander {
  JiraConnectorDTO jiraConnectorDTO;
  JiraAction jiraAction;
  String project;
  String summary;
  String description;
  String issueType;
  String priority;
  List<String> labels;
  Map<String, JiraCustomFieldValue> customFields;

  String issueId;
  List<String> updateIssueIds;
  String status;
  String comment;
  String createmetaExpandParam;
  List<EncryptedDataDetail> encryptionDetails;

  String accountId;
  String appId;
  String activityId;
  String approvalId;

  String approvalField;
  String approvalValue;
  String rejectionField;
  String rejectionValue;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return JiraCapabilityGenerator.generateDelegateCapabilities(jiraConnectorDTO, encryptionDetails, maskingEvaluator);
  }
}
