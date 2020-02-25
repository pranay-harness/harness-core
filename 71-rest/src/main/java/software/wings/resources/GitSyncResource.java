package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.yaml.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;

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
   * List.
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("errors")
  public RestResponse<PageResponse<GitSyncError>> list(
      @BeanParam PageRequest<GitSyncError> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    pageRequest.addFilter(GitSyncErrorKeys.accountId, SearchFilter.Operator.HAS, accountId);
    PageResponse<GitSyncError> pageResponse = gitSyncService.list(pageRequest);
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
    gitSyncService.discardGitSyncErrorsForGivenIds(accountId, errors);
    return RestResponse.Builder.aRestResponse().build();
  }
}
