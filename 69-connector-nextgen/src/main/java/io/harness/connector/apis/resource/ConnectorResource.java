package io.harness.connector.apis.resource;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.NGPageResponse;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestWrapper;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.apis.dto.ConnectorWrapper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hibernate.validator.constraints.NotEmpty;

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
  public ResponseDTO<ConnectorWrapper> get(@NotEmpty @PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("connectorIdentifier") String connectorIdentifier) {
    Optional<ConnectorDTO> connectorDTO =
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return ResponseDTO.newResponse(createConnectorWrapper(connectorDTO.orElse(null)));
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
      @QueryParam("category") ConnectorCategory category) {
    return ResponseDTO.newResponse(getNGPageResponse(connectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category)));
  }

  @POST
  @ApiOperation(value = "Creates a Connector", nickname = "createConnector")
  public ResponseDTO<ConnectorWrapper> create(@NotNull @Valid ConnectorRequestWrapper connectorRequestDTO,
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier) {
    ConnectorDTO connectorDTO = connectorService.create(connectorRequestDTO.getConnector(), accountIdentifier);
    return ResponseDTO.newResponse(createConnectorWrapper(connectorDTO));
  }

  @PUT
  @ApiOperation(value = "Updates a Connector", nickname = "updateConnector")
  public ResponseDTO<ConnectorWrapper> update(@NotNull @Valid ConnectorRequestWrapper connectorRequestDTO,
      @NotEmpty @PathParam("accountIdentifier") String accountIdentifier) {
    ConnectorDTO connectorDTO = connectorService.update(connectorRequestDTO.getConnector(), accountIdentifier);
    return ResponseDTO.newResponse(createConnectorWrapper(connectorDTO));
  }

  private ConnectorWrapper createConnectorWrapper(ConnectorDTO connectorDTO) {
    if (connectorDTO != null) {
      return ConnectorWrapper.builder().connector(connectorDTO).build();
    }
    return null;
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
  @Deprecated
  @ApiOperation(value = "Get the connectivity status of the Connector", nickname = "getConnectorStatus")
  public ResponseDTO<ConnectorValidationResult> validate(
      ConnectorRequestWrapper connectorDTO, @PathParam("accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.validate(connectorDTO.getConnector(), accountIdentifier));
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