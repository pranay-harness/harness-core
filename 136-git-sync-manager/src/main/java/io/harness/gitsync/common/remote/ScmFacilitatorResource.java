package io.harness.gitsync.common.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PAGE_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.SIZE_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.gitsync.common.YamlConstants;
import io.harness.gitsync.common.dtos.CreatePRDTO;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.dtos.SaasGitDTO;
import io.harness.gitsync.common.impl.GitUtils;
import io.harness.gitsync.common.service.ScmFacilitatorService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.utils.URLDecoderUtility;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotBlank;

@Api("/scm")
@Path("/scm")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "Scm", description = "Contains APIs related to Scm")
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = BAD_REQUEST_CODE, description = BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
    })
@NextGenManagerAuth
@OwnedBy(DX)
public class ScmFacilitatorResource {
  private final ScmOrchestratorService scmOrchestratorService;
  private final ScmFacilitatorService scmFacilitatorService;

  @Inject
  public ScmFacilitatorResource(
      ScmOrchestratorService scmOrchestratorService, ScmFacilitatorService scmFacilitatorService) {
    this.scmOrchestratorService = scmOrchestratorService;
    this.scmFacilitatorService = scmFacilitatorService;
  }

  @GET
  @Path("listRepoBranches")
  @ApiOperation(value = "Gets list of branches by Connector Identifier", nickname = "getListOfBranchesByConnector")
  @Operation(operationId = "getListOfBranchesByConnector",
      summary = "Lists Branches of given Repo by referenced Connector Identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "This contains a list of Branches specific to Referenced Connector Id")
      })
  public ResponseDTO<List<String>>
  listBranchesForRepo(@Parameter(description = "Connector Identifier Reference") @QueryParam(
                          NGCommonEntityConstants.CONNECTOR_IDENTIFIER_REF) String connectorIdentifierRef,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Parameter(description = "Repo Url") @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL,
      @Parameter(description = PAGE_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue(
          "0") int pageNum,
      @Parameter(description = SIZE_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.SIZE) @DefaultValue(
          "50") int pageSize,
      @Parameter(description = GitSyncApiConstants.SEARCH_TERM_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm) {
    return ResponseDTO.newResponse(scmFacilitatorService.listBranchesUsingConnector(accountIdentifier, orgIdentifier,
        projectIdentifier, connectorIdentifierRef, URLDecoderUtility.getDecodedString(repoURL),
        PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm));
  }

  @GET
  @Path("listBranchesByGitConfig")
  @ApiOperation(value = "Retrieves a list of Git Branches corresponding to a Git Sync Config Id",
      nickname = "getListOfBranchesByGitConfig")
  @Operation(operationId = "getListOfBranchesByGitConfig", summary = "Lists Branches by given Git Sync Config Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "This contains a list of Branches specific to Git Sync Config Id")
      })
  public ResponseDTO<List<String>>
  listBranchesForRepo(@Parameter(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) @QueryParam(
                          YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Parameter(description = PAGE_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue(
          "0") int pageNum,
      @Parameter(description = SIZE_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.SIZE) @DefaultValue(
          "50") int pageSize,
      @Parameter(description = GitSyncApiConstants.SEARCH_TERM_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm) {
    return ResponseDTO.newResponse(scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listBranchesForRepoByGitSyncConfig(accountIdentifier, orgIdentifier,
            projectIdentifier, yamlGitConfigIdentifier,
            PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm),
        projectIdentifier, orgIdentifier, accountIdentifier));
  }

  @GET
  @Path("fileContent")
  @ApiOperation(value = "Gets file content", nickname = "getFileContent")
  @Operation(operationId = "getFileContent", summary = "Gets Git File Content",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Git File Content including: object Id and content")
      })
  public ResponseDTO<GitFileContent>
  getFileContent(@Parameter(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE, required = true) @NotBlank @NotNull
                 @QueryParam(YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Parameter(description = "File Path") @QueryParam(YamlConstants.FILE_PATH) @NotBlank @NotNull String filePath,
      @Parameter(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) @QueryParam(YamlConstants.BRANCH)
      String branch, @Parameter(description = "Commit Id") @QueryParam(YamlConstants.COMMIT_ID) String commitId) {
    return ResponseDTO.newResponse(scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.getFileContent(
            yamlGitConfigIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, filePath, branch, commitId),
        projectIdentifier, orgIdentifier, accountIdentifier));
  }

  @POST
  @Path("isSaasGit")
  @ApiOperation(value = "Checks if Saas is possible", nickname = "isSaasGit")
  @Operation(operationId = "isSaasGit", summary = "Checks if Saas is possible for given Repo Url",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "True if Saas is possible for given Repo Url")
      })
  public ResponseDTO<SaasGitDTO>
  isSaasGit(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = "Repo Url") @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL) {
    return ResponseDTO.newResponse(GitUtils.isSaasGit(URLDecoderUtility.getDecodedString(repoURL)));
  }

  @POST
  @Path("createPR")
  @ApiOperation(value = "creates a pull request", nickname = "createPR")
  @Operation(operationId = "createPR", summary = "creates a Pull Request",
      responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Successfully created a PR") })
  public ResponseDTO<CreatePRDTO>
  createPR(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
               NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @RequestBody(description = "Details to create a PR",
          required = true) @Valid @NotNull GitPRCreateRequest gitCreatePRRequest) {
    return ResponseDTO.newResponse(scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.createPullRequest(gitCreatePRRequest),
        gitCreatePRRequest.getProjectIdentifier(), gitCreatePRRequest.getOrgIdentifier(),
        gitCreatePRRequest.getAccountIdentifier()));
  }
}
