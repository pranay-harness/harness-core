package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class JiraInternalConfig {
  String jiraUrl;
  String username;
  @ToString.Exclude String password;

  public String getJiraUrl() {
    return jiraUrl.endsWith("/") ? jiraUrl : jiraUrl.concat("/");
  }
}
