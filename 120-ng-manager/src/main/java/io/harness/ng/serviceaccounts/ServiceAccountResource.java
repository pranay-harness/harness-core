/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.serviceaccounts;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGResourceFilterConstants.IDENTIFIER;
import static io.harness.NGResourceFilterConstants.IDENTIFIERS;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.SERVICEACCOUNT;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.accesscontrol.PlatformPermissions;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.ServiceAccountFilterDTO;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.serviceaccounts.dto.ServiceAccountAggregateDTO;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;
import io.harness.serviceaccount.ServiceAccountDTO;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Optional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("serviceaccount")
@Path("serviceaccount")
@Produces({"application/json", "application/yaml", "text/plain"})
@Consumes({"application/json", "application/yaml", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
@OwnedBy(PL)
public class ServiceAccountResource {
  @Inject private final ServiceAccountService serviceAccountService;

  @POST
  @ApiOperation(value = "Create service account", nickname = "createServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.EDIT_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<ServiceAccountDTO> createServiceAccount(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Valid ServiceAccountDTO serviceAccountRequestDTO) {
    ServiceAccountDTO serviceAccountDTO = serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    return ResponseDTO.newResponse(serviceAccountDTO);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update service account", nickname = "updateServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.EDIT_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<ServiceAccountDTO> updateServiceAccount(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @PathParam(IDENTIFIER) @ResourceIdentifier String identifier,
      @Valid ServiceAccountDTO serviceAccountRequestDTO) {
    ServiceAccountDTO serviceAccountDTO = serviceAccountService.updateServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, serviceAccountRequestDTO);
    return ResponseDTO.newResponse(serviceAccountDTO);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete service account", nickname = "deleteServiceAccount")
  @NGAccessControlCheck(
      resourceType = SERVICEACCOUNT, permission = PlatformPermissions.DELETE_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<Boolean>
  deleteServiceAccount(@NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @PathParam(IDENTIFIER) @ResourceIdentifier String identifier) {
    boolean deleted =
        serviceAccountService.deleteServiceAccount(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(deleted);
  }

  @GET
  @ApiOperation(value = "List service account", nickname = "listServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.VIEW_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<List<ServiceAccountDTO>> listServiceAccounts(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Optional @QueryParam(IDENTIFIERS) List<String> identifiers) {
    List<ServiceAccountDTO> requestDTOS =
        serviceAccountService.listServiceAccounts(accountIdentifier, orgIdentifier, projectIdentifier, identifiers);
    return ResponseDTO.newResponse(requestDTOS);
  }

  @GET
  @Path("aggregate")
  @ApiOperation(value = "List service account", nickname = "listAggregatedServiceAccounts")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.VIEW_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<PageResponse<ServiceAccountAggregateDTO>> listAggregatedServiceAccounts(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Optional @QueryParam(IDENTIFIERS) List<String> identifiers, @BeanParam PageRequest pageRequest,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    ServiceAccountFilterDTO filterDTO = ServiceAccountFilterDTO.builder()
                                            .accountIdentifier(accountIdentifier)
                                            .orgIdentifier(orgIdentifier)
                                            .projectIdentifier(projectIdentifier)
                                            .searchTerm(searchTerm)
                                            .identifiers(identifiers)
                                            .build();
    PageResponse<ServiceAccountAggregateDTO> requestDTOS = serviceAccountService.listAggregateServiceAccounts(
        accountIdentifier, orgIdentifier, projectIdentifier, identifiers, getPageRequest(pageRequest), filterDTO);
    return ResponseDTO.newResponse(requestDTOS);
  }

  @GET
  @Path("aggregate/{identifier}")
  @ApiOperation(value = "Get service account", nickname = "getAggregatedServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.VIEW_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<ServiceAccountAggregateDTO> getAggregatedServiceAccount(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @PathParam(IDENTIFIER) @ResourceIdentifier String identifier) {
    ServiceAccountAggregateDTO aggregateDTO = serviceAccountService.getServiceAccountAggregateDTO(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(aggregateDTO);
  }
}
