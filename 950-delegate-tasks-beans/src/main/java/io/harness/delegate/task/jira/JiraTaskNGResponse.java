package io.harness.delegate.task.jira;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.jira.JiraIssueCreateMetadataNG;
import io.harness.jira.JiraIssueNG;
import io.harness.jira.JiraIssueUpdateMetadataNG;
import io.harness.jira.JiraProjectBasicNG;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JiraTaskNGResponse implements DelegateTaskNotifyResponseData {
  List<JiraProjectBasicNG> projects;
  JiraIssueNG issue;
  JiraIssueCreateMetadataNG issueCreateMetadata;
  JiraIssueUpdateMetadataNG issueUpdateMetadata;

  DelegateMetaInfo delegateMetaInfo;
}
