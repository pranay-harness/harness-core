/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.account.accesscontrol.AccountAccessControlPermissions.VIEW_ACCOUNT_PERMISSION;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_ORGANIZATION_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_USERGROUP_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.ACCOUNT;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.ORGANIZATION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.USERGROUP;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.AggregateAccountResourceService;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.api.AggregateUserGroupService;
import io.harness.ng.core.dto.AccountResourcesDTO;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupAggregateDTO;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("aggregate")
@Path("aggregate")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class NGAggregateResource {
  private final AggregateOrganizationService aggregateOrganizationService;
  private final AggregateProjectService aggregateProjectService;
  private final AggregateUserGroupService aggregateUserGroupService;
  private final OrganizationService organizationService;
  private final AccessControlClient accessControlClient;
  private final AggregateAccountResourceService aggregateAccountResourceService;

  @GET
  @Path("projects/{identifier}")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @ApiOperation(value = "Gets a ProjectAggregateDTO by identifier", nickname = "getProjectAggregateDTO")
  public ResponseDTO<ProjectAggregateDTO> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @OrgIdentifier @io.harness.ng.core.OrgIdentifier String orgIdentifier) {
    return ResponseDTO.newResponse(
        aggregateProjectService.getProjectAggregateDTO(accountIdentifier, orgIdentifier, identifier));
  }

  @GET
  @Path("projects")
  @ApiOperation(value = "Get ProjectAggregateDTO list", nickname = "getProjectAggregateDTOList")
  public ResponseDTO<PageResponse<ProjectAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @QueryParam(NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Set<String> permittedOrgIds = organizationService.getPermittedOrganizations(accountIdentifier, orgIdentifier);
    ProjectFilterDTO projectFilterDTO = getProjectFilterDTO(searchTerm, permittedOrgIds, hasModule, moduleType);
    return ResponseDTO.newResponse(getNGPageResponse(aggregateProjectService.listProjectAggregateDTO(
        accountIdentifier, getPageRequest(pageRequest), projectFilterDTO)));
  }

  private ProjectFilterDTO getProjectFilterDTO(
      String searchTerm, Set<String> orgIdentifiers, boolean hasModule, ModuleType moduleType) {
    return ProjectFilterDTO.builder()
        .searchTerm(searchTerm)
        .orgIdentifiers(orgIdentifiers)
        .hasModule(hasModule)
        .moduleType(moduleType)
        .build();
  }

  @GET
  @Path("organizations/{identifier}")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = VIEW_ORGANIZATION_PERMISSION)
  @ApiOperation(value = "Gets an OrganizationAggregateDTO by identifier", nickname = "getOrganizationAggregateDTO")
  public ResponseDTO<OrganizationAggregateDTO> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, identifier));
  }

  @GET
  @Path("organizations")
  @ApiOperation(value = "Get OrganizationAggregateDTO list", nickname = "getOrganizationAggregateDTOList")
  public ResponseDTO<PageResponse<OrganizationAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    OrganizationFilterDTO organizationFilterDTO = OrganizationFilterDTO.builder().searchTerm(searchTerm).build();
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder harnessManagedOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.harnessManaged, SortOrder.OrderType.DESC).build();
      SortOrder nameOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.name, SortOrder.OrderType.ASC).build();
      organizationFilterDTO.setIgnoreCase(true);
      pageRequest.setSortOrders(ImmutableList.of(harnessManagedOrder, nameOrder));
    }
    return ResponseDTO.newResponse(getNGPageResponse(aggregateOrganizationService.listOrganizationAggregateDTO(
        accountIdentifier, getPageRequest(pageRequest), organizationFilterDTO)));
  }

  @GET
  @Path("acl/usergroups")
  @ApiOperation(value = "Get Aggregated User Group list", nickname = "getUserGroupAggregateList")
  public ResponseDTO<PageResponse<UserGroupAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @BeanParam PageRequest pageRequest,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("userSize") @DefaultValue("6") @Max(20) int userSize) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USERGROUP, null), VIEW_USERGROUP_PERMISSION);
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    return ResponseDTO.newResponse(aggregateUserGroupService.listAggregateUserGroups(
        pageRequest, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, userSize));
  }

  @POST
  @Path("acl/usergroups/filter")
  @ApiOperation(value = "Get Aggregated User Group list with filter", nickname = "getUserGroupAggregateListsWithFilter")
  public ResponseDTO<List<UserGroupAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body AggregateACLRequest aggregateACLRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USERGROUP, null), VIEW_USERGROUP_PERMISSION);
    return ResponseDTO.newResponse(aggregateUserGroupService.listAggregateUserGroups(
        accountIdentifier, orgIdentifier, projectIdentifier, aggregateACLRequest));
  }

  @GET
  @Path("acl/usergroups/{identifier}")
  @ApiOperation(value = "Get Aggregated User Group", nickname = "getUserGroupAggregate")
  public ResponseDTO<UserGroupAggregateDTO> list(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USERGROUP, identifier), VIEW_USERGROUP_PERMISSION);
    return ResponseDTO.newResponse(aggregateUserGroupService.getAggregatedUserGroup(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Path("/account-resources")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  @ApiOperation(value = "Gets count of account resources", nickname = "getAccountResourcesCount")
  public ResponseDTO<AccountResourcesDTO> getAccountResourcesCount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(aggregateAccountResourceService.getAccountResourcesDTO(accountIdentifier));
  }
}
