package io.harness.cdng.jira.resources.service;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.dto.JiraApprovalDTO;
import io.harness.cdng.jira.resources.response.dto.JiraFieldDTO;
import io.harness.cdng.jira.resources.response.dto.JiraGetCreateMetadataDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueTypeDTO;
import io.harness.cdng.jira.resources.response.dto.JiraProjectDTO;

import java.util.List;

public interface JiraResourceService {
  boolean validateCredentials(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier);
  String createTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, CreateJiraTicketRequest request);
  String updateTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, UpdateJiraTicketRequest request);
  JiraIssueDTO fetchIssue(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String jiraIssueId);
  List<JiraProjectDTO> getProjects(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier);
  List<JiraIssueTypeDTO> getProjectStatuses(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String projectKey);
  List<JiraFieldDTO> getFieldsOptions(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String projectKey);
  JiraApprovalDTO checkApproval(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier,
      String issueId, String approvalField, String approvalFieldValue, String rejectionField,
      String rejectionFieldValue);
  JiraGetCreateMetadataDTO getCreateMetadata(IdentifierRef jiraConnectorRef, String orgIdentifier,
      String projectIdentifier, String projectKey, String createExpandParam);
}
