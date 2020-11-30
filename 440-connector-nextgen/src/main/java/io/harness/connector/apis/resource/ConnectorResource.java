package io.harness.connector.apis.resource;

import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.connector.apis.dto.ConnectorCatalogueResponseDTO;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.entities.Connector.Scope;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import com.google.inject.name.Named;
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
import org.hibernate.validator.constraints.NotEmpty;

@Api("/connectors")
@Path("/connectors")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class ConnectorResource {
  private final ConnectorService connectorService;
  private static final String CATEGORY_KEY = "category";

  @Inject
  public ConnectorResource(@Named("connectorDecoratorService") ConnectorService connectorService) {
    this.connectorService = connectorService;
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Connector", nickname = "getConnector")
  public ResponseDTO<ConnectorResponseDTO> get(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier).orElse(null));
  }

  @GET
  @Path("validateUniqueIdentifier")
  @ApiOperation(value = "Validate Identifier is unique", nickname = "validateTheIdentifierIsUnique")
  public ResponseDTO<Boolean> validateTheIdentifierIsUnique(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier) {
    return ResponseDTO.newResponse(connectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @GET
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorList")
  public ResponseDTO<PageResponse<ConnectorResponseDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.TYPE_KEY) ConnectorType type,
      @QueryParam(CATEGORY_KEY) ConnectorCategory category) {
    return ResponseDTO.newResponse(getNGPageResponse(connectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category)));
  }

  @POST
  @ApiOperation(value = "Creates a Connector", nickname = "createConnector")
  public ResponseDTO<ConnectorResponseDTO> create(@Valid @NotNull ConnectorDTO connector,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.create(connector, accountIdentifier));
  }

  @PUT
  @ApiOperation(value = "Updates a Connector", nickname = "updateConnector")
  public ResponseDTO<ConnectorResponseDTO> update(@NotNull @Valid ConnectorDTO connector,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.update(connector, accountIdentifier));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a connector by identifier", nickname = "deleteConnector")
  public ResponseDTO<Boolean> delete(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @POST
  @Path("validate")
  @Deprecated
  @ApiOperation(value = "Get the connectivity status of the Connector", nickname = "getConnectorStatus")
  public ResponseDTO<ConnectorValidationResult> validate(
      ConnectorDTO connector, @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.validate(connector, accountIdentifier));
  }

  @POST
  @Path("testConnection/{identifier}")
  @ApiOperation(value = "Test the connection", nickname = "getTestConnectionResult")
  public ResponseDTO<ConnectorValidationResult> testConnection(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.testConnection(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @GET
  @Path("catalogue")
  @ApiOperation(value = "Get Connector Catalogue", nickname = "getConnectorCatalogue")
  public ResponseDTO<ConnectorCatalogueResponseDTO> getConnectorCatalogue(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.getConnectorCatalogue());
  }

  @GET
  @Path("/stats")
  @ApiOperation(value = "Get Connectors statistics", nickname = "getConnectorStatistics")
  public ResponseDTO<ConnectorStatistics> getConnectorStats(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SCOPE) Scope scope) {
    return ResponseDTO.newResponse(
        connectorService.getConnectorStatistics(accountIdentifier, orgIdentifier, projectIdentifier, scope));
  }
}
