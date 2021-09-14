/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
