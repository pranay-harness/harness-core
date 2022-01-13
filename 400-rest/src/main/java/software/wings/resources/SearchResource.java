/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.exception.HintException;
import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;

import software.wings.app.MainConfiguration;
import software.wings.search.SearchService;
import software.wings.search.framework.AdvancedSearchQuery;
import software.wings.search.framework.SearchResults;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Api("search")
@Path("/search")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class SearchResource {
  @Inject private SearchService searchService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration configuration;

  @GET
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<SearchResults> getSearchResults(
      @QueryParam("query") @NotBlank String searchQuery, @QueryParam("accountId") @NotBlank String accountId) {
    if (isSearchEnabled(accountId)) {
      return new RestResponse<>(searchService.getSearchResults(searchQuery, accountId));
    }
    throw new HintException(String.format("Feature not allowed for account: %s ", accountId));
  }

  @Path("advanced")
  @POST
  @AuthRule(permissionType = LOGGED_IN)
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
