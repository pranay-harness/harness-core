package io.harness.connector.apis.resource;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.NGPageResponse;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Optional;
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

@Api("/connectors")
@Path("accounts/{accountIdentifier}/connectors")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
public class ConnectorResource {
  private final ConnectorService connectorService;

  @Inject
  public ConnectorResource(@Named("connectorDecoratorService") ConnectorService connectorService) {
    this.connectorService = connectorService;
  }

  @GET
  @Path("{connectorIdentifier}")
  @ApiOperation(value = "Get Connector", nickname = "getConnector")
  public ResponseDTO<Optional<ConnectorDTO>> get(@NotEmpty @PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("connectorIdentifier") String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @GET
  @Path("validateUniqueIdentifier")
  @ApiOperation(value = "Validate Identifier is unique", nickname = "validateTheIdentifierIsUnique")
  public ResponseDTO<Boolean> validateTheIdentifierIsUnique(
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("connectorIdentifier") String connectorIdentifier) {
    return ResponseDTO.newResponse(connectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @GET
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorList")
  public ResponseDTO<NGPageResponse<ConnectorSummaryDTO>> list(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size,
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("searchTerm") String searchTerm, @QueryParam("type") ConnectorType type,
      @QueryParam("categories") List<ConnectorCategory> categories) {
    return ResponseDTO.newResponse(getNGPageResponse(connectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, categories)));
  }

  @POST
  @ApiOperation(value = "Creates a Connector", nickname = "createConnector")
  public ResponseDTO<ConnectorDTO> create(@NotNull @Valid ConnectorRequestDTO connectorRequestDTO,
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.create(connectorRequestDTO, accountIdentifier));
  }

  @PUT
  @ApiOperation(value = "Updates a Connector", nickname = "updateConnector")
  public ResponseDTO<ConnectorDTO> update(@NotNull @Valid ConnectorRequestDTO connectorRequestDTO,
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.update(connectorRequestDTO, accountIdentifier));
  }

  @DELETE
  @Path("{connectorIdentifier}")
  @ApiOperation(value = "Delete a connector by identifier", nickname = "deleteConnector")
  public ResponseDTO<Boolean> delete(@PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("connectorIdentifier") String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @POST
  @Path("validate")
  @ApiOperation(value = "Get the connectivity status of the Connector", nickname = "getConnectorStatus")
  public ResponseDTO<ConnectorValidationResult> validate(
      ConnectorRequestDTO connectorDTO, @PathParam("accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.validate(connectorDTO, accountIdentifier));
  }

  @GET
  @Path("testConnection/{connectorIdentifier}")
  @ApiOperation(value = "Test the connection", nickname = "getTestConnectionResult")
  public ResponseDTO<ConnectorValidationResult> testConnection(
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("connectorIdentifier") String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.testConnection(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }
}
