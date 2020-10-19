package io.harness.delegate.task.jira;

import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.JiraCapabilityGenerator;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class JiraTaskNGParameters implements TaskParameters, ExecutionCapabilityDemander {
  private JiraConnectorDTO jiraConnectorDTO;
  private JiraAction jiraAction;
  private String project;
  private String summary;
  private String description;
  private String issueType;
  private String priority;
  private List<String> labels;
  private Map<String, JiraCustomFieldValue> customFields;

  private String issueId;
  private List<String> updateIssueIds;
  private String status;
  private String comment;
  private String createmetaExpandParam;
  List<EncryptedDataDetail> encryptionDetails;

  private String accountId;
  private String appId;
  private String activityId;
  private String approvalId;

  private String approvalField;
  private String approvalValue;
  private String rejectionField;
  private String rejectionValue;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return JiraCapabilityGenerator.generateDelegateCapabilities(jiraConnectorDTO, encryptionDetails);
  }
}
