/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegate.task.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class JiraTaskNGHelper {
  private final JiraTaskNGHandler jiraTaskNGHandler;
  private final SecretDecryptionService secretDecryptionService;

  @Inject
  public JiraTaskNGHelper(JiraTaskNGHandler jiraTaskNGHandler, SecretDecryptionService secretDecryptionService) {
    this.jiraTaskNGHandler = jiraTaskNGHandler;
    this.secretDecryptionService = secretDecryptionService;
  }

  public JiraTaskNGResponse getJiraTaskResponse(JiraTaskNGParameters params) {
    decryptRequestDTOs(params);
    switch (params.getAction()) {
      case VALIDATE_CREDENTIALS:
        return jiraTaskNGHandler.validateCredentials(params);
      case GET_PROJECTS:
        return jiraTaskNGHandler.getProjects(params);
      case GET_STATUSES:
        return jiraTaskNGHandler.getStatuses(params);
      case GET_ISSUE:
        return jiraTaskNGHandler.getIssue(params);
      case GET_ISSUE_CREATE_METADATA:
        return jiraTaskNGHandler.getIssueCreateMetadata(params);
      case GET_ISSUE_UPDATE_METADATA:
        return jiraTaskNGHandler.getIssueUpdateMetadata(params);
      case CREATE_ISSUE:
        return jiraTaskNGHandler.createIssue(params);
      case UPDATE_ISSUE:
        return jiraTaskNGHandler.updateIssue(params);
      default:
        throw new InvalidRequestException(String.format("Invalid jira action: %s", params.getAction()));
    }
  }

  private void decryptRequestDTOs(JiraTaskNGParameters jiraTaskNGParameters) {
    secretDecryptionService.decrypt(
        jiraTaskNGParameters.getJiraConnectorDTO(), jiraTaskNGParameters.getEncryptionDetails());
  }
}
