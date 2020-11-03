package io.harness.cdng.jira.resources.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.jira.JiraIssueType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraProjectDTO {
  String id;
  String key;
  String name;
  @JsonProperty("issuetypes") List<JiraIssueType> issueTypes;
}
