package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.GitCommit;
import software.wings.beans.GitDetail;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.yaml.GitToHarnessErrorCommitStats;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.yaml.errorhandling.GitProcessingError;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;

import java.util.List;
import javax.annotation.Nullable;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by vardanb
 */
@Api("git-sync")
@Path("git-sync")
@Produces(APPLICATION_JSON)
@Scope(SETTING)
@Slf4j
public class GitSyncResource {
  private GitSyncService gitSyncService;
  private GitSyncErrorService gitSyncErrorService;

  @Inject
  public GitSyncResource(GitSyncService gitSyncService, GitSyncErrorService gitSyncErrorService) {
    this.gitSyncService = gitSyncService;
    this.gitSyncErrorService = gitSyncErrorService;
  }

  @GET
  @Path("errors/count")
  @Timed
  @ExceptionMetered
  public RestResponse<Long> gitSyncErrorCount(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(gitSyncErrorService.getTotalGitErrorsCount(accountId));
  }

  /**
   * List errors
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("errors")
  public RestResponse<PageResponse<GitSyncError>> listErrors(
      @BeanParam PageRequest<GitSyncError> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    pageRequest.addFilter(GitSyncErrorKeys.accountId, EQ, accountId);
    PageResponse<GitSyncError> pageResponse = gitSyncErrorService.fetchErrors(pageRequest);
    return new RestResponse<>(pageResponse);
  }

  /**
   * List git to harness errors
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("errors/gitToHarness")
  public RestResponse<PageResponse<GitToHarnessErrorCommitStats>> listGitToHarnessErrors(
      @BeanParam PageRequest<GitToHarnessErrorCommitStats> pageRequest,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("gitConnectorId") String gitConnectorId,
      @QueryParam("branchName") String branchName) {
    pageRequest.addFilter(GitSyncErrorKeys.accountId, EQ, accountId);
    PageResponse<GitToHarnessErrorCommitStats> pageResponse =
        gitSyncErrorService.fetchGitToHarnessErrors(pageRequest, accountId, gitConnectorId, branchName);
    return new RestResponse<>(pageResponse);
  }

  /**
   * List git to harness errors for a particular Commit
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("errors/gitToHarness/{commitId}")
  public RestResponse<PageResponse<GitSyncError>> listGitToHarnessErrorsForACommit(
      @BeanParam PageRequest<GitSyncError> pageRequest, @PathParam("commitId") String commitId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("includeData") List<String> includeDataList,
      @QueryParam("yamlFilePathPattern") String yamlFilePathPattern) {
    PageResponse<GitSyncError> pageResponse = gitSyncErrorService.fetchErrorsInEachCommits(
        pageRequest, commitId, accountId, includeDataList, yamlFilePathPattern);
    return new RestResponse<>(pageResponse);
  }

  /**
   * List harness to git errors
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("errors/harnessToGit")
  public RestResponse<PageResponse<GitSyncError>> listHarnessToGitErrors(
      @BeanParam PageRequest<GitSyncError> pageRequest, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("gitConnectorId") String gitConnectorId, @QueryParam("branchName") String branchName) {
    PageResponse<GitSyncError> pageResponse =
        gitSyncErrorService.fetchHarnessToGitErrors(pageRequest, accountId, gitConnectorId, branchName);
    return new RestResponse<>(pageResponse);
  }

  /**
   * List Processing Errors
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("errors/connectivityIssue")
  public RestResponse<PageResponse<GitProcessingError>> listGitConnectivityIssues(
      @BeanParam PageRequest<GitProcessingError> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    PageResponse<GitProcessingError> processingErrors =
        gitSyncErrorService.fetchGitConnectivityIssues(pageRequest, accountId);
    return new RestResponse<>(processingErrors);
  }

  /**
   * List activity
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("activities")
  public RestResponse<PageResponse<GitFileActivity>> listGitFileActivity(
      @BeanParam PageRequest<GitFileActivity> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    pageRequest.addFilter(GitFileActivityKeys.accountId, EQ, accountId);
    PageResponse<GitFileActivity> pageResponse = gitSyncService.fetchGitSyncActivity(pageRequest);
    return new RestResponse<>(pageResponse);
  }

  /**
   *
   * @param accountId
   * @param errors
   * @return
   */
  @POST
  @Path("errors/_discard")
  public RestResponse discardGitSyncErrorV2(@QueryParam("accountId") String accountId, List<String> errors) {
    gitSyncErrorService.deleteGitSyncErrorAndLogFileActivity(errors, Status.DISCARDED, accountId);
    return RestResponse.Builder.aRestResponse().build();
  }

  /**
   *
   * @param accountId
   * @return
   */
  @GET
  @Path("repos")
  @Timed
  @ExceptionMetered
  public RestResponse<List<GitDetail>> listRepositories(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(gitSyncService.fetchRepositories(accountId));
  }

  /**
   * List commits
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("commits")
  public RestResponse<PageResponse<GitCommit>> listCommits(@BeanParam PageRequest<GitCommit> pageRequest,
      @QueryParam("gitToHarness") @Nullable Boolean gitToHarness, @QueryParam("accountId") @NotEmpty String accountId) {
    PageResponse<GitCommit> pageResponse = gitSyncService.fetchGitCommits(pageRequest, gitToHarness, accountId);
    return new RestResponse<>(pageResponse);
  }
}
