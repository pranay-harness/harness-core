package io.harness.connector.apis.resource;

import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

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

@Api("/connectorFilters")
@Path("/connectorFilters")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ConnectorFilterResource {
  private ConnectorFilterService connectorFilterService;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Connector Filter", nickname = "getConnectorFilter")
  public ResponseDTO<ConnectorFilterDTO> get(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotEmpty @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    return ResponseDTO.newResponse(
        connectorFilterService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @ApiOperation(value = "Get Connector Filter", nickname = "getConnectorFilterList")
  public ResponseDTO<PageResponse<ConnectorFilterDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(getNGPageResponse(
        connectorFilterService.list(page, size, accountIdentifier, orgIdentifier, projectIdentifier, null)));
  }

  @POST
  @ApiOperation(value = "Creates a Connector Filter", nickname = "postConnectorFilter")
  public ResponseDTO<ConnectorFilterDTO> create(@Valid @NotNull ConnectorFilterDTO connectorFilter,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorFilterService.create(accountIdentifier, connectorFilter));
  }

  @PUT
  @ApiOperation(value = "Updates a Connector Filter", nickname = "updateConnectorFilter")
  public ResponseDTO<ConnectorFilterDTO> update(@NotNull @Valid ConnectorFilterDTO connectorFilter,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorFilterService.update(accountIdentifier, connectorFilter));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a connector filter", nickname = "deleteConnectorFilter")
  public ResponseDTO<Boolean> delete(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotEmpty @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    return ResponseDTO.newResponse(
        connectorFilterService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
