/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.YamlConstants;
import io.harness.gitsync.common.dtos.GitEntityBranchSummaryFilterDTO;
import io.harness.gitsync.common.dtos.GitEntitySummaryFilterDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncRepoFilesListDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api("/git-sync-entities")
@Path("/git-sync-entities")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitEntityResource {
  private GitEntityService gitEntityService;
  @GET
  @Path("{entityType}")
  @ApiOperation(value = "Get Git Sync Entity By Type", nickname = "listGitSyncEntitiesByType")
  public ResponseDTO<PageResponse<GitSyncEntityListDTO>> listByType(
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationIdentifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @QueryParam(YamlConstants.GITSYNC_CONFIG_ID) String gitSyncConfigId,
      @QueryParam(YamlConstants.BRANCH) String branch,
      @PathParam(NGCommonEntityConstants.ENTITY_TYPE) EntityType entityType,
      @QueryParam(NGCommonEntityConstants.PAGE) @DefaultValue("0") int page,
      @QueryParam(NGCommonEntityConstants.SIZE) int size,
      @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType) {
    // Added moduleType for now if in future we want to support product filter in entities as well.
    return ResponseDTO.newResponse(gitEntityService.getPageByType(
        projectIdentifier, organizationIdentifier, accountIdentifier, gitSyncConfigId, branch, entityType, page, size));
  }

  @POST
  @Path("summary")
  @ApiOperation(value = "List Git Sync Entity by product for List of Repos and Entities",
      nickname = "listGitSyncEntitiesSummaryForRepoAndTypes")
  public ResponseDTO<GitSyncRepoFilesListDTO>
  listSummary(@QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationIdentifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.SIZE) int size, @Body GitEntitySummaryFilterDTO gitEntityFilter) {
    return ResponseDTO.newResponse(gitEntityService.listSummary(projectIdentifier, organizationIdentifier,
        accountIdentifier, gitEntityFilter.getModuleType(), gitEntityFilter.getSearchTerm(),
        gitEntityFilter.getGitSyncConfigIdentifiers(), gitEntityFilter.getEntityTypes(), size));
  }

  @POST
  @Path("branch/{branch}")
  @ApiOperation(value = "List Git Sync Entity by product for Repo and Branch and List of Entities",
      nickname = "listGitSyncEntitiesSummaryForRepoAndBranch")
  public ResponseDTO<List<GitSyncEntityListDTO>>
  listSummaryByRepoAndBranch(@QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationIdentifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.SIZE) int size,
      @QueryParam(YamlConstants.GITSYNC_CONFIG_ID) String gitSyncConfigId,
      @PathParam(YamlConstants.BRANCH) @NotEmpty String branch, @Body GitEntityBranchSummaryFilterDTO gitEntityFilter) {
    return ResponseDTO.newResponse(gitEntityService.listSummaryByRepoAndBranch(projectIdentifier,
        organizationIdentifier, accountIdentifier, gitEntityFilter.getModuleType(), gitEntityFilter.getSearchTerm(),
        gitSyncConfigId, branch, gitEntityFilter.getEntityTypes(), size));
  }
}
