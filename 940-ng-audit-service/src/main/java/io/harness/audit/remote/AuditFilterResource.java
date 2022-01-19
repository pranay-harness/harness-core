/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.audit.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filter.FilterType.AUDIT;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.impl.AuditPermissionValidator;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.ResourceScopeDTO;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@OwnedBy(HarnessTeam.PL)
@Api("auditFilters")
@Path("auditFilters")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "AuditFilters", description = "This contains APIs related to Filter of type Audit as defined in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
public class AuditFilterResource {
  private FilterService filterService;
  private AuditPermissionValidator auditPermissionValidator;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Filter of type Audit", nickname = "getAuditFilter")
  @Operation(operationId = "getAuditFilter", summary = "Gets a Filter of type Audit by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns Filter of type Audit and having filterIdentifier as specified in request")
      })
  public ResponseDTO<FilterDTO>
  get(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(description = "Filter Identifier", required = true) @NotNull @PathParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    auditPermissionValidator.validate(accountIdentifier,
        ResourceScopeDTO.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .build());
    return ResponseDTO.newResponse(
        filterService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier, AUDIT));
  }

  @GET
  @ApiOperation(value = "Get Filter list of type Audit", nickname = "getAuditFilterList")
  @Operation(operationId = "getAuditFilterList",
      summary = "Get the list of Filters of type Audit satisfying the criteria (if any) in the request",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of Filters of type Audit")
      })
  public ResponseDTO<PageResponse<FilterDTO>>
  list(@Parameter(description = "Page number of navigation. If left empty, default value of 0 is assumed") @QueryParam(
           NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @Parameter(description = "Number of entries per page. If left empty, default value of 100 is assumed")
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    auditPermissionValidator.validate(accountIdentifier,
        ResourceScopeDTO.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .build());
    return ResponseDTO.newResponse(getNGPageResponse(
        filterService.list(page, size, accountIdentifier, orgIdentifier, projectIdentifier, null, AUDIT)));
  }

  @POST
  @ApiOperation(value = "Create a Filter of type Audit", nickname = "postAuditFilter")
  @Operation(operationId = "postAuditFilter", summary = "Creates a Filter",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns created Filter")
      })
  public ResponseDTO<FilterDTO>
  create(@RequestBody(
             required = true, description = "Details of the Filter to create") @Valid @NotNull FilterDTO filterDTO,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    AuditFilterPropertiesDTO auditFilterProperties = (AuditFilterPropertiesDTO) filterDTO.getFilterProperties();
    if (auditFilterProperties != null && isNotEmpty(auditFilterProperties.getScopes())) {
      for (ResourceScopeDTO resourceScopeDTO : auditFilterProperties.getScopes()) {
        auditPermissionValidator.validate(accountIdentifier, resourceScopeDTO);
      }
    }
    return ResponseDTO.newResponse(filterService.create(accountIdentifier, filterDTO));
  }

  @PUT
  @ApiOperation(value = "Update a Filter of type Audit", nickname = "updateAuditFilter")
  @Operation(operationId = "updateAuditFilter", summary = "Updates the Filter of type Audit",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the updated Filter")
      })
  public ResponseDTO<FilterDTO>
  update(@RequestBody(required = true,
             description = "This is the updated Filter. This should have all the fields not just the updated ones")
         @NotNull @Valid FilterDTO filterDTO,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    AuditFilterPropertiesDTO auditFilterProperties = (AuditFilterPropertiesDTO) filterDTO.getFilterProperties();
    if (auditFilterProperties != null && isNotEmpty(auditFilterProperties.getScopes())) {
      for (ResourceScopeDTO resourceScopeDTO : auditFilterProperties.getScopes()) {
        auditPermissionValidator.validate(accountIdentifier, resourceScopeDTO);
      }
    }
    return ResponseDTO.newResponse(filterService.update(accountIdentifier, filterDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a filter of type Audit", nickname = "deleteAuditFilter")
  @Operation(operationId = "deleteAuditFilter", summary = "Delete a Filter of type Audit by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Boolean status whether request was successful or not")
      })
  public ResponseDTO<Boolean>
  delete(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(description = "Filter Identifier", required = true) @NotNull @PathParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    auditPermissionValidator.validate(accountIdentifier,
        ResourceScopeDTO.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .build());
    return ResponseDTO.newResponse(
        filterService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier, AUDIT));
  }
}
