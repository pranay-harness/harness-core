package io.harness.ng.jira.resources;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.JiraApprovalResponseDTO;
import io.harness.cdng.jira.resources.response.JiraFieldResponseDTO;
import io.harness.cdng.jira.resources.response.JiraGetCreateMetadataResponseDTO;
import io.harness.cdng.jira.resources.response.JiraProjectResponseDTO;
import io.harness.cdng.jira.resources.response.JiraProjectStatusesResponseDTO;
import io.harness.cdng.jira.resources.response.dto.JiraApprovalDTO;
import io.harness.cdng.jira.resources.response.dto.JiraFieldDTO;
import io.harness.cdng.jira.resources.response.dto.JiraGetCreateMetadataDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueTypeDTO;
import io.harness.cdng.jira.resources.response.dto.JiraProjectDTO;
import io.harness.cdng.jira.resources.service.JiraResourceService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("jira")
@Path("/jira")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class JiraResource {
  private final JiraResourceService jiraResourceService;

  @GET
  @Path("validate")
  @ApiOperation(value = "Validate jira credentials", nickname = "validateJiraCredentials")
  public ResponseDTO<Boolean> validate(@QueryParam("connectorRef") String jiraConnectorIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    boolean isValid = jiraResourceService.validateCredentials(connectorRef, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(isValid);
  }

  @POST
  @Path("createTicket")
  @ApiOperation(value = "Create jira ticket", nickname = "createJiraTicket")
  public ResponseDTO<String> createTicket(@QueryParam("connectorRef") String jiraConnectorIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier, CreateJiraTicketRequest request) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    String ticketKey = jiraResourceService.createTicket(connectorRef, orgIdentifier, projectIdentifier, request);
    return ResponseDTO.newResponse(ticketKey);
  }

  @POST
  @Path("updateTicket")
  @ApiOperation(value = "Update jira ticket", nickname = "updateJiraTicket")
  public ResponseDTO<String> updateTicket(@QueryParam("connectorRef") String jiraConnectorIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier, UpdateJiraTicketRequest request) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    String ticketKey = jiraResourceService.updateTicket(connectorRef, orgIdentifier, projectIdentifier, request);
    return ResponseDTO.newResponse(ticketKey);
  }

  @GET
  @Path("fetchIssue")
  @ApiOperation(value = "Fetch jira issue", nickname = "fetchJiraIssue")
  public ResponseDTO<JiraIssueDTO> fetchJiraIssue(@QueryParam("connectorRef") String jiraConnectorIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("jiraIssueId") String jiraIssueId) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    JiraIssueDTO jiraIssueDTO =
        jiraResourceService.fetchIssue(connectorRef, orgIdentifier, projectIdentifier, jiraIssueId);
    return ResponseDTO.newResponse(jiraIssueDTO);
  }

  @GET
  @Path("getProjects")
  @ApiOperation(value = "Get jira projects", nickname = "getJiraProjects")
  public ResponseDTO<JiraProjectResponseDTO> getProjects(@QueryParam("connectorRef") String jiraConnectorIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    List<JiraProjectDTO> jiraProjectDTOList =
        jiraResourceService.getProjects(connectorRef, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(JiraProjectResponseDTO.builder().jiraProjects(jiraProjectDTOList).build());
  }

  @GET
  @Path("getProjectStatuses")
  @ApiOperation(value = "Get jira project statuses", nickname = "getJiraProjectStatuses")
  public ResponseDTO<JiraProjectStatusesResponseDTO> getJiraProjectStatuses(
      @QueryParam("connectorRef") String jiraConnectorIdentifier, @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("projectKey") String projectKey) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    List<JiraIssueTypeDTO> projectStatuses =
        jiraResourceService.getProjectStatuses(connectorRef, orgIdentifier, projectIdentifier, projectKey);
    return ResponseDTO.newResponse(JiraProjectStatusesResponseDTO.builder().jiraIssueTypeList(projectStatuses).build());
  }

  @GET
  @Path("getFieldsOptions")
  @ApiOperation(value = "Get jira fields options", nickname = "getJiraFieldsOptions")
  public ResponseDTO<JiraFieldResponseDTO> getJiraFieldsOptions(
      @QueryParam("connectorRef") String jiraConnectorIdentifier, @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("projectKey") String projectKey) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    List<JiraFieldDTO> fieldsOptions =
        jiraResourceService.getFieldsOptions(connectorRef, orgIdentifier, projectIdentifier, projectKey);
    return ResponseDTO.newResponse(JiraFieldResponseDTO.builder().jiraFields(fieldsOptions).build());
  }

  @GET
  @Path("checkApproval")
  @ApiOperation(value = "Check jira approval", nickname = "checkJiraApproval")
  public ResponseDTO<JiraApprovalResponseDTO> checkJiraApproval(
      @QueryParam("connectorRef") String jiraConnectorIdentifier, @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("issueId") String issueId, @QueryParam("approvalField") String approvalField,
      @QueryParam("approvalFieldValue") String approvalFieldValue, @QueryParam("rejectionField") String rejectionField,
      @QueryParam("rejectionFieldValue") String rejectionFieldValue) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    JiraApprovalDTO jiraApprovalDTO = jiraResourceService.checkApproval(connectorRef, orgIdentifier, projectIdentifier,
        issueId, approvalField, approvalFieldValue, rejectionField, rejectionFieldValue);
    return ResponseDTO.newResponse(JiraApprovalResponseDTO.builder().jiraApproval(jiraApprovalDTO).build());
  }

  @GET
  @Path("getCreateMetadata")
  @ApiOperation(value = "Get jira create metadata options", nickname = "getJiraCreateMetadata")
  public ResponseDTO<JiraGetCreateMetadataResponseDTO> getJiraCreateMetadata(
      @QueryParam("connectorRef") String jiraConnectorIdentifier, @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("projectKey") String projectKey, @QueryParam("createExpandParam") String createExpandParam) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    JiraGetCreateMetadataDTO jiraGetCreateMetadataDTO = jiraResourceService.getCreateMetadata(
        connectorRef, orgIdentifier, projectIdentifier, projectKey, createExpandParam);
    return ResponseDTO.newResponse(
        JiraGetCreateMetadataResponseDTO.builder().jiraGetCreateMetadata(jiraGetCreateMetadataDTO).build());
  }
}
