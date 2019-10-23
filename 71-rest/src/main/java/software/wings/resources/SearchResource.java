package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.HintException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureName;
import software.wings.search.framework.SearchResults;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SearchService;

import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("search")
@Path("/search")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class SearchResource {
  @Inject SearchService searchService;
  @Inject FeatureFlagService featureFlagService;
  @Inject MainConfiguration configuration;

  @GET
  @Timed
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SearchResults> getSearchResults(@QueryParam("query") @NotBlank String searchQuery,
      @QueryParam("accountId") @NotBlank String accountId) throws IOException {
    if (!featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, accountId)) {
      throw new HintException(String.format("Feature not allowed for account: %s ", accountId));
    } else if (!configuration.isSearchEnabled()) {
      throw new HintException("Search is not enabled for your account");
    }
    return new RestResponse<>(searchService.getSearchResults(searchQuery, accountId));
  }
}