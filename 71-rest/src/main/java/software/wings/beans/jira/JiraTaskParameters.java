package software.wings.beans.jira;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.JiraConfig;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class JiraTaskParameters {
  private JiraConfig jiraConfig;
  private JiraAction jiraAction;
  private String project;
  private String summary;
  private String description;
  private String issueType;
  private String priority;
  private List<String> labels;
  private Map<String, Object> customFields;

  private String issueId;
  private String status;
  private String comment;
  private String createmetaExpandParam;
  List<EncryptedDataDetail> encryptionDetails;

  private String accountId;
  private String appId;
  private String activityId;
  private String approvalId;

  private String webhookUrl;
  private String approvalField;
  private String approvalValue;
  private String rejectionField;
  private String rejectionValue;
  private String callbackUrl;
}
