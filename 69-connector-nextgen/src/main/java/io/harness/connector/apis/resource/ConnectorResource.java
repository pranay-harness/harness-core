package io.harness.connector.apis.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

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
@Path("/connectors")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorResource {
  private ConnectorService connectorService;

  @GET
  @Path("{connectorIdentifier}")
  @ApiOperation(value = "Get Connector", nickname = "getConnector")
  public Optional<ConnectorDTO> get(@NotEmpty @QueryParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("connectorIdentifier") String connectorIdentifier) {
    // todo @deepak: Make the changes to use accountIdentifier in pathparam
    return connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @GET
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorList")
  public Page<ConnectorSummaryDTO> list(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size, ConnectorFilter connectorFilter,
      @NotEmpty @QueryParam("accountIdentifier") String accountIdentifier) {
    return connectorService.list(connectorFilter, page, size, accountIdentifier);
  }

  @POST
  @ApiOperation(value = "Creates a Connector", nickname = "createConnector")
  public ConnectorDTO create(@NotNull @Valid ConnectorRequestDTO connectorRequestDTO,
      @NotEmpty @QueryParam("accountIdentifier") String accountIdentifier) {
    return connectorService.create(connectorRequestDTO, accountIdentifier);
  }

  @PUT
  @ApiOperation(value = "Updates a Connector", nickname = "updateConnector")
  public ConnectorDTO update(@NotNull @Valid ConnectorRequestDTO connectorRequestDTO,
      @NotEmpty @QueryParam("accountIdentifier") String accountIdentifier) {
    return connectorService.update(connectorRequestDTO, accountIdentifier);
  }

  @DELETE
  @Path("{connectorIdentifier}")
  @ApiOperation(value = "Delete a connector by identifier", nickname = "deleteConnector")
  public boolean delete(@QueryParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("connectorIdentifier") String connectorIdentifier) {
    return connectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @POST
  @Path("validate")
  @ApiOperation(value = "Get the connectivity status of the Connector", nickname = "getConnectorStatus")
  public RestResponse<ConnectorValidationResult> validate(
      ConnectorRequestDTO connectorDTO, @QueryParam("accountIdentifier") String accountIdentifier) {
    return RestResponse.Builder.aRestResponse()
        .withResource(connectorService.validate(connectorDTO, accountIdentifier))
        .build();
  }
}
