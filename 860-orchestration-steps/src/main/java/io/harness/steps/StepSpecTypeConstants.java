package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface StepSpecTypeConstants {
  String SHELL_SCRIPT = "ShellScript";
  String BARRIER = "Barrier";
  String HTTP = "Http";
  String HARNESS_APPROVAL = "HarnessApproval";
  String JIRA_APPROVAL = "JiraApproval";
  String JIRA_CREATE = "JiraCreate";
  String JIRA_UPDATE = "JiraUpdate";
  String RESOURCE_CONSTRAINT = "ResourceConstraint";
  String FLAG_CONFIGURATION = "FlagConfiguration";
  String SERVICENOW_APPROVAL = "ServiceNowApproval";
}
