package io.harness.ng.core.delegate.resources;

import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_DELETE_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_EDIT_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_RESOURCE_TYPE;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_VIEW_PERMISSION;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.delegate.filter.DelegateProfileFilterPropertiesDTO;
import io.harness.ng.core.api.DelegateProfileManagerNgService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Path("/v2")
@Api("/v2")
@Produces("application/json")
@AuthRule(permissionType = LOGGED_IN)
@OwnedBy(HarnessTeam.DEL)
public class DelegateConfigNgV2Resource {
  private final DelegateProfileManagerNgService delegateProfileManagerNgService;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateConfigNgV2Resource(
      DelegateProfileManagerNgService delegateProfileManagerNgService, AccessControlClient accessControlClient) {
    this.delegateProfileManagerNgService = delegateProfileManagerNgService;
    this.accessControlClient = accessControlClient;
  }

  @GET
  @Path("/accounts/{accountId}/delegate-configs/{delegateConfigIdentifier}")
  @ApiOperation(value = "Gets delegate config by identifier", nickname = "getDelegateConfigNgV2")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileDetailsNg> get(
      @PathParam("delegateConfigIdentifier") @NotEmpty String delegateConfigIdentifier,
      @PathParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(
        delegateProfileManagerNgService.get(accountId, orgId, projectId, delegateConfigIdentifier));
  }

  @PUT
  @Path("/accounts/{accountId}/delegate-configs/{delegateConfigIdentifier}/scoping-rules")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Updates the scoping rules inside the delegate config", nickname = "updateScopingRulesNgV2")
  public RestResponse<DelegateProfileDetailsNg> updateScopingRules(
      @PathParam("delegateConfigIdentifier") @NotEmpty String delegateConfigIdentifier,
      @PathParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, List<ScopingRuleDetailsNg> scopingRules) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateConfigIdentifier), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.updateScopingRules(
        accountId, orgId, projectId, delegateConfigIdentifier, scopingRules));
  }

  @DELETE
  @Path("/accounts/{accountId}/delegate-configs/{delegateConfigIdentifier}")
  @ApiOperation(value = "Deletes a delegate config by identifier", nickname = "deleteDelegateConfigNgV2")
  @Timed
  @ExceptionMetered
  public ResponseDTO<Boolean> delete(@PathParam("delegateConfigIdentifier") @NotEmpty String delegateConfigIdentifier,
      @PathParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateConfigIdentifier), DELEGATE_CONFIG_DELETE_PERMISSION);
    return ResponseDTO.newResponse(
        delegateProfileManagerNgService.delete(accountId, orgId, projectId, delegateConfigIdentifier));
  }

  @PUT
  @Path("/accounts/{accountId}/delegate-configs/{delegateConfigIdentifier}/selectors")
  @ApiOperation(value = "Updates the selectors inside the delegate config", nickname = "updateSelectorsNgV2")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileDetailsNg> updateSelectors(
      @PathParam("delegateConfigIdentifier") @NotEmpty String delegateConfigIdentifier,
      @PathParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, List<String> selectors) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateConfigIdentifier), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.updateSelectors(
        accountId, orgId, projectId, delegateConfigIdentifier, selectors));
  }

  @PUT
  @Path("/accounts/{accountId}/delegate-configs/{delegateConfigIdentifier}")
  @ApiOperation(value = "Updates a delegate config", nickname = "updateDelegateConfigNgV2")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileDetailsNg> update(
      @PathParam("delegateConfigIdentifier") @NotEmpty String delegateConfigIdentifier,
      @PathParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, @NotNull DelegateProfileDetailsNg delegateConfig) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateConfigIdentifier), DELEGATE_CONFIG_EDIT_PERMISSION);

    delegateConfig.setAccountId(accountId);
    delegateConfig.setIdentifier(delegateConfigIdentifier);
    delegateConfig.setOrgIdentifier(orgId);
    delegateConfig.setProjectIdentifier(projectId);
    return new RestResponse<>(delegateProfileManagerNgService.updateV2(
        accountId, orgId, projectId, delegateConfigIdentifier, delegateConfig));
  }

  @POST
  @Path("/delegate-configs")
  @ApiOperation(value = "Adds a delegate profile", nickname = "addDelegateProfileNgV2noQueryParamsV2")
  public RestResponse<DelegateProfileDetailsNg> add(@NotNull DelegateProfileDetailsNg delegateProfile) {
    String accountId = delegateProfile.getAccountId();
    String orgId = delegateProfile.getOrgIdentifier();
    String projectId = delegateProfile.getProjectIdentifier();
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.add(delegateProfile));
  }

  @POST
  @Path("/accounts/{accountId}/delegate-configs")
  @ApiOperation(value = "Adds a delegate profile", nickname = "addDelegateProfileNgV2")
  public RestResponse<DelegateProfileDetailsNg> add(
      @PathParam("accountId") @NotEmpty String accountId, @NotNull DelegateProfileDetailsNg delegateProfile) {
    delegateProfile.setAccountId(accountId);
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, delegateProfile.getOrgIdentifier(), delegateProfile.getProjectIdentifier()),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_EDIT_PERMISSION);
    return new RestResponse<>(delegateProfileManagerNgService.add(delegateProfile));
  }

  @GET
  @ApiOperation(value = "Lists the delegate configs", nickname = "listDelegateConfigsNgV2")
  @Timed
  @Path("/accounts/{accountId}/delegate-configs")
  @ExceptionMetered
  public RestResponse<PageResponse<DelegateProfileDetailsNg>> list(
      @BeanParam PageRequest<DelegateProfileDetailsNg> pageRequest, @PathParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.list(accountId, pageRequest, orgId, projectId));
  }

  @POST
  @ApiOperation(value = "Lists the delegate configs with filter", nickname = "listDelegateConfigsNgV2WithFilter")
  @Timed
  @Path("/accounts/{accountId}/delegate-configs/listV2")
  @ExceptionMetered
  public RestResponse<PageResponse<DelegateProfileDetailsNg>> listV2(@PathParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId,
      @QueryParam(NGResourceFilterConstants.FILTER_KEY) String filterIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @Body DelegateProfileFilterPropertiesDTO delegateProfileFilterPropertiesDTO,
      @BeanParam PageRequest<DelegateProfileDetailsNg> pageRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.listV2(
        accountId, orgId, projectId, filterIdentifier, searchTerm, delegateProfileFilterPropertiesDTO, pageRequest));
  }
}
