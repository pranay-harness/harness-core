/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.rbac.ProjectPermissions.VIEW_PROJECT_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.connector.accesscontrol.ResourceTypes;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorAggregateByCommitDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorCountDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
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
import javax.validation.constraints.Max;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

@Api("git-sync-errors")
@Path("git-sync-errors")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "Git Sync Errors", description = "Contains APIs related to Git Sync Errors")
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
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@OwnedBy(PL)
public class GitSyncErrorResource {
  private GitSyncErrorService gitSyncErrorService;

  @GET
  @Path("/aggregate")
  @ApiOperation(value = "Gets Error list grouped by commit", nickname = "listGitToHarnessErrorsCommits")
  @Operation(operationId = "listGitToHarnessErrorsGroupedByCommits",
      summary = "Lists Git to Harness Errors grouped by Commits for the given scope, Repo and Branch",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Paginated list of Git to Harness Errors grouped by Commit")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitSyncErrorAggregateByCommitDTO>>
  listGitToHarnessErrorsGroupedByCommits(
      @RequestBody(description = "Details of Page including: size, index, sort") @BeanParam PageRequest pageRequest,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier @OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.
      harness.accesscontrol.ProjectIdentifier @ProjectIdentifier String projectIdentifier,
      @Parameter(description = GitSyncApiConstants.SEARCH_TERM_PARAM_MESSAGE) @QueryParam(
          NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @RequestBody(description = "Details to find Git Entity including: Git Sync Config Id and Branch Name")
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @Parameter(description = "Number of errors that will be displayed in the summary") @QueryParam(
          "numberOfErrorsInSummary") @DefaultValue("5") @Max(5) Integer numberOfErrorsInSummary) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(GitSyncErrorKeys.createdAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    return ResponseDTO.newResponse(gitSyncErrorService.listGitToHarnessErrorsGroupedByCommits(pageRequest,
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, gitEntityBasicInfo.getYamlGitConfigId(),
        gitEntityBasicInfo.getBranch(), numberOfErrorsInSummary));
  }

  @GET
  @Path("/commits/{commitId}")
  @ApiOperation(value = "Gets Error list for a particular Commit", nickname = "listGitToHarnessErrorsForCommit")
  @Operation(operationId = "listGitToHarnessErrorForCommit",
      summary = "Lists Git to Harness Errors for the given Commit Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Paginated list of Git to Harness Errors for given Commit Id")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitSyncErrorDTO>>
  listGitSyncErrorsForACommit(
      @RequestBody(description = "Details of Page including: size, index, sort") @BeanParam PageRequest pageRequest,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.
      harness.accesscontrol.ProjectIdentifier @ProjectIdentifier String projectIdentifier,
      @RequestBody(description = "Details to find Git Entity including: Git Sync Config Id and Branch Name")
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @Parameter(description = "Commit Id", required = true) @PathParam("commitId") String commitId) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(GitSyncErrorKeys.createdAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    return ResponseDTO.newResponse(
        gitSyncErrorService.listGitToHarnessErrorsForCommit(pageRequest, commitId, accountIdentifier, orgIdentifier,
            projectIdentifier, gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getBranch()));
  }

  @GET
  @ApiOperation(value = "Gets Error list", nickname = "listGitSyncErrors")
  @Operation(operationId = "listGitSyncErrors",
      summary = "Lists Git to Harness Errors by file or connectivity errors for the given scope, Repo and Branch",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Paginated list of Git to Harness Errors by file or connectivity errors")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitSyncErrorDTO>>
  listGitSyncErrors(
      @RequestBody(description = "Details of Page including: size, index, sort") @BeanParam PageRequest pageRequest,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.
      harness.accesscontrol.ProjectIdentifier @ProjectIdentifier String projectIdentifier,
      @Parameter(description = GitSyncApiConstants.SEARCH_TERM_PARAM_MESSAGE) @QueryParam(
          NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @RequestBody(description = "Details to find Git Entity including: Git Sync Config Id and Branch Name")
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @Parameter(
          description =
              "This specifies which errors to show - (Git to Harness or Connectivity), Put true to show Git to Harness Errors")
      @QueryParam("gitToHarness") @DefaultValue("true") Boolean gitToHarnessErrors) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(GitSyncErrorKeys.createdAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }

    if (Boolean.TRUE.equals(gitToHarnessErrors)) {
      return ResponseDTO.newResponse(
          gitSyncErrorService.listAllGitToHarnessErrors(pageRequest, accountIdentifier, orgIdentifier,
              projectIdentifier, searchTerm, gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getBranch()));
    } else {
      return ResponseDTO.newResponse(gitSyncErrorService.listConnectivityErrors(
          accountIdentifier, orgIdentifier, projectIdentifier, gitEntityBasicInfo.getYamlGitConfigId(), pageRequest));
    }
  }

  @GET
  @Path("/count")
  @ApiOperation(value = "Gets Error Count", nickname = "getGitSyncErrorsCount")
  @Operation(operationId = "getGitSyncErrorsCount", summary = "Get Errors Count for the given scope, Repo and Branch",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Count of both Git to Harness Errors and Connectivity Errors")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<GitSyncErrorCountDTO>
  getErrorCount(
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.
      harness.accesscontrol.ProjectIdentifier @ProjectIdentifier String projectIdentifier,
      @Parameter(description = GitSyncApiConstants.SEARCH_TERM_PARAM_MESSAGE) @QueryParam(
          NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @RequestBody(description = "Details to find Git Entity including: Git Sync Config Id and Branch Name")
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    return ResponseDTO.newResponse(gitSyncErrorService.getErrorCount(accountIdentifier, orgIdentifier,
        projectIdentifier, searchTerm, gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getBranch()));
  }
}
