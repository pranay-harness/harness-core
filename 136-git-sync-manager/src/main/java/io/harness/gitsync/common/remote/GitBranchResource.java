package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.YamlConstants;
import io.harness.gitsync.common.dtos.GitBranchDTO;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.utils.URLDecoderUtility;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/git-sync-branch")
@Path("/git-sync-branch")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitBranchResource {
  private final GitBranchService gitBranchService;

  @GET
  @Path("listRepoBranches")
  @ApiOperation(value = "Gets list of branches by Connector Identifier", nickname = "getListOfBranchesByConnector")
  public ResponseDTO<List<String>> listBranchesForRepo(
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL,
      @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue("0") int pageNum,
      @QueryParam(NGCommonEntityConstants.SIZE) @DefaultValue("50") int pageSize,
      @QueryParam(NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm) {
    return ResponseDTO.newResponse(gitBranchService.listBranchesForRepoByConnector(accountIdentifier, orgIdentifier,
        projectIdentifier, connectorIdentifier, URLDecoderUtility.getDecodedString(repoURL),
        PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm));
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
    return ResponseDTO.newResponse(
        gitBranchService.listBranchesForRepoByGitSyncConfig(accountIdentifier, orgIdentifier, projectIdentifier,
            yamlGitConfigIdentifier, PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm));
  }

  @GET
  @Path("listBranchesWithStatus")
  @ApiOperation(value = "Gets list of branches with their status by Git Config Identifier",
      nickname = "getListOfBranchesWithStatus")
  public ResponseDTO<PageResponse<GitBranchDTO>>
  listBranchesWithStatusForRepo(@NotEmpty @QueryParam(YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue("0") int pageNum,
      @QueryParam(NGCommonEntityConstants.SIZE) int pageSize,
      @QueryParam(NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm) {
    return ResponseDTO.newResponse(
        gitBranchService.listBranchesWithStatus(accountIdentifier, orgIdentifier, projectIdentifier,
            yamlGitConfigIdentifier, PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(), searchTerm));
  }

  @POST
  @Path("sync")
  @ApiOperation(value = "Sync the new branch into harness", nickname = "syncGitBranch")
  public ResponseDTO<Boolean> listBranchesWithStatusForRepo(
      @NotEmpty @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY) String yamlGitConfigIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(GitSyncApiConstants.BRANCH_KEY) String branchName) {
    return ResponseDTO.newResponse(gitBranchService.syncNewBranch(
        accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier, branchName));
  }
}
