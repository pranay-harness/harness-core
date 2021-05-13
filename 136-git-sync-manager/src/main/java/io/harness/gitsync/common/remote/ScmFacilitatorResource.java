package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.YamlConstants;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.dtos.SaasGitDTO;
import io.harness.gitsync.common.impl.GitUtils;
import io.harness.gitsync.common.service.ScmOrchestratorService;
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
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
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
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class ScmFacilitatorResource {
  private final ScmOrchestratorService scmOrchestratorService;

  @GET
  @Path("listRepoBranches")
  @ApiOperation(value = "Gets list of branches by Connector Identifier", nickname = "getListOfBranchesByConnector")
  public ResponseDTO<List<String>> listBranchesForRepo(
      @QueryParam(NGCommonEntityConstants.CONNECTOR_IDENTIFIER_REF) String connectorIdentifierRef,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL,
      @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue("0") int pageNum,
      @QueryParam(NGCommonEntityConstants.SIZE) @DefaultValue("50") int pageSize,
      @QueryParam(NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm) {
    return ResponseDTO.newResponse(scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listBranchesForRepoByConnector(accountIdentifier, orgIdentifier,
            projectIdentifier, connectorIdentifierRef, URLDecoderUtility.getDecodedString(repoURL),
            PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm),
        projectIdentifier, orgIdentifier, accountIdentifier));
  }

  @GET
  @Path("listBranchesByGitConfig")
  @ApiOperation(value = "Gets list of branches by Git Config Identifier", nickname = "getListOfBranchesByGitConfig")
  public ResponseDTO<List<String>> listBranchesForRepo(
      @QueryParam(YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue("0") int pageNum,
      @QueryParam(NGCommonEntityConstants.SIZE) @DefaultValue("50") int pageSize,
      @QueryParam(NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm) {
    return ResponseDTO.newResponse(scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listBranchesForRepoByGitSyncConfig(accountIdentifier, orgIdentifier,
            projectIdentifier, yamlGitConfigIdentifier,
            PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm),
        projectIdentifier, orgIdentifier, accountIdentifier));
  }

  @GET
  @Path("fileContent")
  @ApiOperation(value = "Gets file content", nickname = "getFileContent")
  public ResponseDTO<GitFileContent> getFileContent(
      @NotBlank @NotNull @QueryParam(YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @NotBlank @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(YamlConstants.FILE_PATH) @NotBlank @NotNull String filePath,
      @QueryParam(YamlConstants.BRANCH) String branch, @QueryParam(YamlConstants.COMMIT_ID) String commitId) {
    return ResponseDTO.newResponse(scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.getFileContent(
            yamlGitConfigIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, filePath, branch, commitId),
        projectIdentifier, orgIdentifier, accountIdentifier));
  }

  @POST
  @Path("isSaasGit")
  @ApiOperation(value = "Checks if Saas is possible", nickname = "isSaasGit")
  public ResponseDTO<SaasGitDTO> isSaasGit(@QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL) {
    return ResponseDTO.newResponse(GitUtils.isSaasGit(repoURL));
  }
}
