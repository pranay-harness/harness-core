package io.harness.delegate.resources;

import static io.harness.delegate.utils.RbacConstants.DELEGATE_EDIT_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_RESOURCE_TYPE;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_VIEW_PERMISSION;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.delegate.filter.DelegateFilterPropertiesDTO;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateSetupService;

import software.wings.security.annotations.AuthRule;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api("/setup/delegates/ng/v2")
@Path("/setup/delegates/ng/v2")
@Produces("application/json")
@AuthRule(permissionType = LOGGED_IN)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupResourceV2 {
  private final DelegateSetupService delegateSetupService;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateSetupResourceV2(DelegateSetupService delegateSetupService, AccessControlClient accessControlClient) {
    this.delegateSetupService = delegateSetupService;
    this.accessControlClient = accessControlClient;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateGroupListing> list(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_VIEW_PERMISSION);

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateSetupService.listDelegateGroupDetails(accountId, orgId, projectId));
    }
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateGroupListing> listV2(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId,
      @QueryParam(NGResourceFilterConstants.FILTER_KEY) String filterIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @Body DelegateFilterPropertiesDTO delegateFilterPropertiesDTO,
      @BeanParam PageRequest<DelegateGroupDetails> pageRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_VIEW_PERMISSION);

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateSetupService.listDelegateGroupDetailsV2(
          accountId, orgId, projectId, filterIdentifier, searchTerm, delegateFilterPropertiesDTO, pageRequest));
    }
  }

  @GET
  @Path("up-the-hierarchy")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateGroupListing> listUpTheHierarchy(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_VIEW_PERMISSION);

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          delegateSetupService.listDelegateGroupDetailsUpTheHierarchy(accountId, orgId, projectId));
    }
  }

  @GET
  @Path("{identifier}")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateGroupDetails> get(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_VIEW_PERMISSION);

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          delegateSetupService.getDelegateGroupDetailsV2(accountId, orgId, projectId, identifier));
    }
  }

  @PUT
  @Path("{identifier}")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateGroupDetails> update(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, DelegateGroupDetails delegateGroupDetails) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, identifier), DELEGATE_EDIT_PERMISSION);

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          delegateSetupService.updateDelegateGroup(accountId, orgId, projectId, identifier, delegateGroupDetails));
    }
  }
}
