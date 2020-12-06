package software.wings.service.impl.analysis;

import software.wings.beans.jira.JiraTaskParameters;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CVCollaborationProviderParameters {
  String collaborationProviderConfigId;
  JiraTaskParameters jiraTaskParameters;
  CVFeedbackRecord cvFeedbackRecord;
}
