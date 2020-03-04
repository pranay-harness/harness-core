package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.beans.GitCommit;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.yaml.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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

  @Inject
  public GitSyncResource(GitSyncService gitSyncService) {
    this.gitSyncService = gitSyncService;
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
    pageRequest.addFilter(GitSyncErrorKeys.accountId, SearchFilter.Operator.EQ, accountId);
    PageResponse<GitSyncError> pageResponse = gitSyncService.fetchErrors(pageRequest);
    return new RestResponse<>(pageResponse);
  }

  /**
   * List activity
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("activity")
  public RestResponse<PageResponse<GitFileActivity>> listGitFileActivity(
      @BeanParam PageRequest<GitFileActivity> pageRequest, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("filePath") String filePath) {
    pageRequest.addFilter(GitFileActivityKeys.accountId, SearchFilter.Operator.EQ, accountId);
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
  @Path("discard")
  public RestResponse discardGitSyncError(@QueryParam("accountId") String accountId, List<GitSyncError> errors) {
    gitSyncService.updateGitSyncErrorStatus(errors, Status.DISCARDED, accountId);
    return RestResponse.Builder.aRestResponse().build();
  }

  /**
   *
   * @param accountId
   * @return
   */
  @GET
  @Path("apps")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Application>> listRepositories(@QueryParam("accountId") String accountId) {
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
  public RestResponse<PageResponse<GitCommit>> listCommits(
      @BeanParam PageRequest<GitCommit> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    PageResponse<GitCommit> pageResponse = gitSyncService.fetchGitCommits(pageRequest, accountId);
    return new RestResponse<>(pageResponse);
  }
}
