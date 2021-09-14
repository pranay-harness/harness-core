/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.YamlConstants;
import io.harness.gitsync.common.beans.BranchSyncStatus;
import io.harness.gitsync.common.dtos.GitBranchListDTO;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.Max;
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
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitBranchResource {
  private final GitBranchService gitBranchService;

  @GET
  @Path("listBranchesWithStatus")
  @ApiOperation(value = "Gets list of branches with their status by Git Config Identifier",
      nickname = "getListOfBranchesWithStatus")
  public ResponseDTO<GitBranchListDTO>
  listBranchesWithStatusForRepo(@NotEmpty @QueryParam(YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue("0") int pageNum,
      @QueryParam(NGCommonEntityConstants.SIZE) @Max(100) int pageSize,
      @QueryParam(NGCommonEntityConstants.SEARCH_TERM) @DefaultValue("") String searchTerm,
      @QueryParam("branchSyncStatus") BranchSyncStatus branchSyncStatus) {
    return ResponseDTO.newResponse(gitBranchService.listBranchesWithStatus(accountIdentifier, orgIdentifier,
        projectIdentifier, yamlGitConfigIdentifier, PageRequest.builder().pageIndex(pageNum).pageSize(pageSize).build(),
        searchTerm, branchSyncStatus));
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
