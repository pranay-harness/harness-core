package software.wings.beans.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.JiraConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.jira.JiraAction;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Data
@Builder
public class JiraTaskParameters implements ExecutionCapabilityDemander {
  private JiraConfig jiraConfig;
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
    return CapabilityHelper.generateDelegateCapabilities(jiraConfig, encryptionDetails);
  }
}
