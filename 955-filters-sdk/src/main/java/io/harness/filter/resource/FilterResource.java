/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filter.resource;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/filters")
@Path("/filters")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@OwnedBy(DX)
public class FilterResource {
  private FilterService filterService;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Filter", nickname = "getFilter")
  public ResponseDTO<FilterDTO> get(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.TYPE_KEY) FilterType type) {
    return ResponseDTO.newResponse(
        filterService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier, type));
  }

  @GET
  @ApiOperation(value = "Get Filter", nickname = "getFilterList")
  public ResponseDTO<PageResponse<FilterDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.TYPE_KEY) FilterType type) {
    return ResponseDTO.newResponse(getNGPageResponse(
        filterService.list(page, size, accountIdentifier, orgIdentifier, projectIdentifier, null, type)));
  }

  @POST
  @ApiOperation(value = "Create a Filter", nickname = "postFilter")
  public ResponseDTO<FilterDTO> create(@Valid @NotNull FilterDTO filterDTO,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(filterService.create(accountIdentifier, filterDTO));
  }

  @PUT
  @ApiOperation(value = "Update a Filter", nickname = "updateFilter")
  public ResponseDTO<FilterDTO> update(@NotNull @Valid FilterDTO filterDTO,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(filterService.update(accountIdentifier, filterDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a filter", nickname = "deleteFilter")
  public ResponseDTO<Boolean> delete(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.TYPE_KEY) FilterType type) {
    return ResponseDTO.newResponse(
        filterService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier, type));
  }
}
