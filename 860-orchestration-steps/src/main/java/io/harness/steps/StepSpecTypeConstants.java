/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
}
