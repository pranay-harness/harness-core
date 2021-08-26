package io.harness.steps.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraIssueNG;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@JsonTypeName("JiraIssueOutcome")
@TypeAlias("jiraIssueOutcome")
@RecasterAlias("io.harness.steps.jira.JiraIssueOutcome")
public class JiraIssueOutcome extends HashMap<String, Object> implements Outcome {
  public JiraIssueOutcome(JiraIssueNG issue) {
    super(issue.getFields());
  }
}
