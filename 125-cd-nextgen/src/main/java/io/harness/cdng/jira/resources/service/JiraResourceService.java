/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.cdng.jira.resources.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.jira.JiraIssueCreateMetadataNG;
import io.harness.jira.JiraIssueUpdateMetadataNG;
import io.harness.jira.JiraProjectBasicNG;
import io.harness.jira.JiraStatusNG;

import java.util.List;

@OwnedBy(CDC)
public interface JiraResourceService {
  boolean validateCredentials(IdentifierRef jiraConnectorRef, String orgId, String projectId);
  List<JiraProjectBasicNG> getProjects(IdentifierRef jiraConnectorRef, String orgId, String projectId);
  List<JiraStatusNG> getStatuses(
      IdentifierRef jiraConnectorRef, String orgId, String projectId, String projectKey, String issueType);
  JiraIssueCreateMetadataNG getIssueCreateMetadata(IdentifierRef jiraConnectorRef, String orgId, String projectId,
      String projectKey, String issueType, String expand, boolean fetchStatus, boolean ignoreComment);
  JiraIssueUpdateMetadataNG getIssueUpdateMetadata(
      IdentifierRef jiraConnectorRef, String orgId, String projectId, String issueKey);
}
