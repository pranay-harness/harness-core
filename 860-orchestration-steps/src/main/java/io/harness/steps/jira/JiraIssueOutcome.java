package io.harness.steps.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraIssueNG;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@JsonTypeName("JiraIssueOutcome")
@TypeAlias("jiraIssueOutcome")
public class JiraIssueOutcome extends JiraIssueNG implements Outcome {
  public JiraIssueOutcome(JiraIssueNG issue) {
    super(issue.getUrl(), issue.getId(), issue.getKey(), issue.getFields());
  }

  @Override
  public String getType() {
    return "jiraIssueOutcome";
  }
}
