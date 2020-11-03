package io.harness.ng.jira.resources;

import com.google.inject.Inject;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.JiraIssueDTO;
import io.harness.cdng.jira.resources.response.JiraProjectDTO;
import io.harness.cdng.jira.resources.response.JiraProjectResponse;
import io.harness.cdng.jira.resources.service.JiraResourceService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.utils.IdentifierRefHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
  @Path("get-projects")
  @ApiOperation(value = "Get jira projects", nickname = "getJiraProjects")
  public ResponseDTO<JiraProjectResponse> getProjects(@QueryParam("connectorRef") String jiraConnectorIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(jiraConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    List<JiraProjectDTO> jiraProjectDTOList =
        jiraResourceService.getProjects(connectorRef, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(JiraProjectResponse.builder().jiraProjects(jiraProjectDTOList).build());
  }
}
