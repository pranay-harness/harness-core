package io.harness.gitsync.gitsyncerror.remote;

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
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@OwnedBy(PL)
public class GitSyncErrorResource {
  private GitSyncErrorService gitSyncErrorService;

  @GET
  @Path("/aggregate")
  @ApiOperation(value = "Gets Error list grouped by commit", nickname = "listGitToHarnessErrorsCommits")
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitSyncErrorAggregateByCommitDTO>> listGitToHarnessErrorsGroupedByCommits(
      @BeanParam PageRequest pageRequest,
      @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier
      @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("numberOfErrorsInSummary") @DefaultValue("5") @Max(5) Integer numberOfErrorsInSummary) {
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
  @ApiOperation(value = "Gets Error list for a particular commit", nickname = "listGitToHarnessErrorsForCommit")
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitSyncErrorDTO>> listGitSyncErrorsForACommit(@BeanParam PageRequest pageRequest,
      @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier
      @ProjectIdentifier String projectIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, @PathParam("commitId") String commitId) {
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
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitSyncErrorDTO>> listGitSyncErrors(@BeanParam PageRequest pageRequest,
      @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier
      @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
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
      return ResponseDTO.newResponse(gitSyncErrorService.listConnectivityErrors(accountIdentifier, orgIdentifier,
          projectIdentifier, gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getBranch(), pageRequest));
    }
  }

  @GET
  @Path("/count")
  @ApiOperation(value = "Gets Error Count", nickname = "getGitSyncErrorsCount")
  @NGAccessControlCheck(resourceType = ResourceTypes.PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<GitSyncErrorCountDTO> getErrorCount(
      @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @io.harness.accesscontrol.AccountIdentifier String accountIdentifier,
      @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier
      @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    return ResponseDTO.newResponse(gitSyncErrorService.getErrorCount(accountIdentifier, orgIdentifier,
        projectIdentifier, searchTerm, gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getBranch()));
  }
}