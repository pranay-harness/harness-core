package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGResourceFilterConstants.IDENTIFIER;
import static io.harness.NGResourceFilterConstants.IDENTIFIERS;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.SERVICEACCOUNT;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.accesscontrol.PlatformPermissions;
import io.harness.ng.accesscontrol.PlatformResourceTypes;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.ApiKeyAggregateDTO;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.dto.ApiKeyFilterDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.ApiKey.ApiKeyKeys;

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

@Api("apikey")
@Path("apikey")
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
public class ApiKeyResource {
  private final ApiKeyService apiKeyService;
  private final AccessControlClient accessControlClient;

  @POST
  @ApiOperation(value = "Create api key", nickname = "createApiKey")
  public ResponseDTO<ApiKeyDTO> createApiKey(@Valid ApiKeyDTO apiKeyDTO) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(apiKeyDTO.getAccountIdentifier(),
                                                  apiKeyDTO.getOrgIdentifier(), apiKeyDTO.getProjectIdentifier()),
        Resource.of(PlatformResourceTypes.SERVICEACCOUNT, apiKeyDTO.getParentIdentifier()),
        MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION);
    ApiKeyDTO apiKey = apiKeyService.createApiKey(apiKeyDTO);
    return ResponseDTO.newResponse(apiKey);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update api key", nickname = "updateApiKey")
  public ResponseDTO<ApiKeyDTO> updateApiKey(
      @Valid ApiKeyDTO apiKeyDTO, @NotNull @PathParam("identifier") String identifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(apiKeyDTO.getAccountIdentifier(),
                                                  apiKeyDTO.getOrgIdentifier(), apiKeyDTO.getProjectIdentifier()),
        Resource.of(PlatformResourceTypes.SERVICEACCOUNT, apiKeyDTO.getParentIdentifier()),
        MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION);

    ApiKeyDTO apiKey = apiKeyService.updateApiKey(apiKeyDTO);
    return ResponseDTO.newResponse(apiKey);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete api key", nickname = "deleteApiKey")
  public ResponseDTO<Boolean> deleteApiKey(@NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) String orgIdentifier, @Optional @QueryParam(PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @NotNull @PathParam(IDENTIFIER_KEY) String identifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PlatformResourceTypes.SERVICEACCOUNT, parentIdentifier), MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION);

    boolean deleted = apiKeyService.deleteApiKey(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifier);
    return ResponseDTO.newResponse(deleted);
  }

  @GET
  @ApiOperation(value = "List api keys", nickname = "listApiKeys")
  public ResponseDTO<List<ApiKeyDTO>> listApiKeys(@NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) String orgIdentifier, @Optional @QueryParam(PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @Optional @QueryParam(IDENTIFIERS) List<String> identifiers) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PlatformResourceTypes.SERVICEACCOUNT, parentIdentifier), MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION);

    List<ApiKeyDTO> apiKeyDTOs = apiKeyService.listApiKeys(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifiers);
    return ResponseDTO.newResponse(apiKeyDTOs);
  }

  @GET
  @Path("aggregate")
  @ApiOperation(value = "List api key", nickname = "listAggregatedApiKeys")
  @NGAccessControlCheck(
      resourceType = SERVICEACCOUNT, permission = PlatformPermissions.MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<PageResponse<ApiKeyAggregateDTO>>
  listAggregatedApiKeys(@NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @Optional @QueryParam(IDENTIFIERS) List<String> identifiers, @BeanParam PageRequest pageRequest,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ApiKeyKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    ApiKeyFilterDTO filterDTO = ApiKeyFilterDTO.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .parentIdentifier(parentIdentifier)
                                    .apiKeyType(apiKeyType)
                                    .searchTerm(searchTerm)
                                    .identifiers(identifiers)
                                    .build();
    PageResponse<ApiKeyAggregateDTO> requestDTOS =
        apiKeyService.listAggregateApiKeys(accountIdentifier, getPageRequest(pageRequest), filterDTO);
    return ResponseDTO.newResponse(requestDTOS);
  }

  @GET
  @Path("aggregate/{identifier}")
  @ApiOperation(value = "Get api key", nickname = "getAggregatedApiKey")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = MANAGEAPIKEY_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<ApiKeyAggregateDTO> getAggregatedApiKey(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @PathParam(IDENTIFIER) @ResourceIdentifier String identifier) {
    ApiKeyAggregateDTO aggregateDTO = apiKeyService.getApiKeyAggregateDTO(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifier);
    return ResponseDTO.newResponse(aggregateDTO);
  }
}
