package io.harness.cdng.jira.resources.request;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.jira.JiraCustomFieldValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateJiraTicketRequest {
  @NotNull String project;
  @NotNull String summary;
  @NotNull String description;
  @NotNull String issueType;
  String priority;
  List<String> labels;
  Map<String, JiraCustomFieldValue> customFields;
  String issueId;
  List<String> updateIssueIds;
  String status;
  String comment;
  String createmetaExpandParam;
  String activityId;
  String approvalId;
  String approvalField;
  String approvalValue;
  String rejectionField;
  String rejectionValue;
}
