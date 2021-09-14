/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGResourceFilterConstants.IDENTIFIERS;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.api.TokenService;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.TokenAggregateDTO;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.dto.TokenFilterDTO;
import io.harness.ng.core.entities.Token.TokenKeys;
import io.harness.security.annotations.InternalApi;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Optional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.Instant;
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

@Api("token")
@Path("token")
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
public class TokenResource {
  private final TokenService tokenService;
  private final ApiKeyService apiKeyService;

  @POST
  @ApiOperation(value = "Create token", nickname = "createToken")
  public ResponseDTO<String> createToken(@Valid TokenDTO tokenDTO) {
    apiKeyService.validateParentIdentifier(tokenDTO.getAccountIdentifier(), tokenDTO.getOrgIdentifier(),
        tokenDTO.getProjectIdentifier(), tokenDTO.getApiKeyType(), tokenDTO.getParentIdentifier());
    return ResponseDTO.newResponse(tokenService.createToken(tokenDTO));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update token", nickname = "updateToken")
  public ResponseDTO<TokenDTO> updateToken(@PathParam("identifier") String identifier, @Valid TokenDTO tokenDTO) {
    apiKeyService.validateParentIdentifier(tokenDTO.getAccountIdentifier(), tokenDTO.getOrgIdentifier(),
        tokenDTO.getProjectIdentifier(), tokenDTO.getApiKeyType(), tokenDTO.getParentIdentifier());
    return ResponseDTO.newResponse(tokenService.updateToken(tokenDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete token", nickname = "deleteToken")
  public ResponseDTO<Boolean> deleteToken(@PathParam("identifier") String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @NotNull @QueryParam("apiKeyIdentifier") String apiKeyIdentifier) {
    apiKeyService.validateParentIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier);
    return ResponseDTO.newResponse(tokenService.revokeToken(accountIdentifier, orgIdentifier, projectIdentifier,
        apiKeyType, parentIdentifier, apiKeyIdentifier, identifier));
  }

  @GET
  @InternalApi
  @ApiOperation(value = "Get token", nickname = "getToken")
  public ResponseDTO<TokenDTO> getToken(@QueryParam("tokenId") String tokenId) {
    return ResponseDTO.newResponse(tokenService.getToken(tokenId, true));
  }

  @POST
  @Path("rotate/{identifier}")
  @ApiOperation(value = "Rotate token", nickname = "rotateToken")
  public ResponseDTO<String> rotateToken(@PathParam("identifier") String identifier,
      @QueryParam("rotateTimestamp") Long rotateTimestamp,
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @NotNull @QueryParam("apiKeyIdentifier") String apiKeyIdentifier) {
    apiKeyService.validateParentIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier);
    return ResponseDTO.newResponse(tokenService.rotateToken(accountIdentifier, orgIdentifier, projectIdentifier,
        apiKeyType, parentIdentifier, apiKeyIdentifier, identifier, Instant.ofEpochMilli(rotateTimestamp)));
  }

  @GET
  @Path("aggregate")
  @ApiOperation(value = "List tokens", nickname = "listAggregatedTokens")
  public ResponseDTO<PageResponse<TokenAggregateDTO>> listAggregatedTokens(
      @NotNull @QueryParam(ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam("apiKeyType") ApiKeyType apiKeyType,
      @NotNull @QueryParam("parentIdentifier") String parentIdentifier,
      @NotNull @QueryParam("apiKeyIdentifier") String apiKeyIdentifier,
      @Optional @QueryParam(IDENTIFIERS) List<String> identifiers, @BeanParam PageRequest pageRequest,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    apiKeyService.validateParentIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier);
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(TokenKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    TokenFilterDTO filterDTO = TokenFilterDTO.builder()
                                   .accountIdentifier(accountIdentifier)
                                   .orgIdentifier(orgIdentifier)
                                   .projectIdentifier(projectIdentifier)
                                   .parentIdentifier(parentIdentifier)
                                   .apiKeyType(apiKeyType)
                                   .searchTerm(searchTerm)
                                   .apiKeyIdentifier(apiKeyIdentifier)
                                   .identifiers(identifiers)
                                   .build();
    PageResponse<TokenAggregateDTO> requestDTOS =
        tokenService.listAggregateTokens(accountIdentifier, getPageRequest(pageRequest), filterDTO);
    return ResponseDTO.newResponse(requestDTOS);
  }
}
