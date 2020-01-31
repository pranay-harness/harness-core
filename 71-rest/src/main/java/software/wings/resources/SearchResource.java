package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import io.harness.exception.HintException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureName;
import software.wings.search.SearchService;
import software.wings.search.framework.AdvancedSearchQuery;
import software.wings.search.framework.SearchResults;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.FeatureFlagService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("search")
@Path("/search")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class SearchResource {
  @Inject private SearchService searchService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration configuration;

  @GET
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<SearchResults> getSearchResults(
      @QueryParam("query") @NotBlank String searchQuery, @QueryParam("accountId") @NotBlank String accountId) {
    if (isSearchEnabled(accountId)) {
      return new RestResponse<>(searchService.getSearchResults(searchQuery, accountId));
    }
    throw new HintException(String.format("Feature not allowed for account: %s ", accountId));
  }

  @Path("advanced")
  @POST
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<SearchResults> getSearchResults(
      @QueryParam("accountId") @NotBlank String accountId, AdvancedSearchQuery advancedSearchQuery) {
    if (isSearchEnabled(accountId)) {
      return new RestResponse<>(searchService.getSearchResults(accountId, advancedSearchQuery));
    }
    throw new HintException(String.format("Feature not allowed for account: %s ", accountId));
  }

  private boolean isSearchEnabled(String accountId) {
    return configuration.isSearchEnabled() && featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, accountId);
  }
}