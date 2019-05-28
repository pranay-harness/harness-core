package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.jira.JiraTaskParameters;

@Data
@Builder
public class CVCollaborationProviderParameters {
  String collaborationProviderConfigId;
  JiraTaskParameters jiraTaskParameters;
  CVFeedbackRecord cvFeedbackRecord;
}
