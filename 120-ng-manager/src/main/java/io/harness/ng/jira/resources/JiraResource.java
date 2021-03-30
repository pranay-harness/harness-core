package io.harness.ng.jira.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.service.JiraResourceService;
import io.harness.jira.JiraIssueCreateMetadataNG;
import io.harness.jira.JiraProjectBasicNG;
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
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(CDC)
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
  public ResponseDTO<Boolean> validateCredentials(@NotNull @QueryParam("connectorRef") String jiraConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId) {
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(jiraConnectorRef, accountId, orgId, projectId);
    boolean isValid = jiraResourceService.validateCredentials(connectorRef, orgId, projectId);
    return ResponseDTO.newResponse(isValid);
  }

  @GET
  @Path("projects")
  @ApiOperation(value = "Get jira projects", nickname = "getJiraProjects")
  public ResponseDTO<List<JiraProjectBasicNG>> getProjects(@NotNull @QueryParam("connectorRef") String jiraConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId) {
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(jiraConnectorRef, accountId, orgId, projectId);
    List<JiraProjectBasicNG> projects = jiraResourceService.getProjects(connectorRef, orgId, projectId);
    return ResponseDTO.newResponse(projects);
  }

  @GET
  @Path("createMetadata")
  @ApiOperation(value = "Get jira issue create metadata", nickname = "getJiraIssueCreateMetadata")
  public ResponseDTO<JiraIssueCreateMetadataNG> getIssueCreateMetadata(
      @NotNull @QueryParam("connectorRef") String jiraConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId, @QueryParam("projectKey") String projectKey,
      @QueryParam("issueKey") String issueKey, @QueryParam("expand") String expand) {
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(jiraConnectorRef, accountId, orgId, projectId);
    JiraIssueCreateMetadataNG createMetadata =
        jiraResourceService.getIssueCreateMetadata(connectorRef, orgId, projectId, projectKey, issueKey, expand);
    return ResponseDTO.newResponse(createMetadata);
  }
}
