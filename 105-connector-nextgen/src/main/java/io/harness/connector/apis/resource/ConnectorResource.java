package io.harness.connector.apis.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorService;
import io.swagger.annotations.Api;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Api("/connectors")
@Path("/connectors")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ConnectorResource {
  public final ConnectorService connectorService;
  public final ConnectorMapper connectorMapper;

  @Inject
  ConnectorResource(ConnectorService connectorService, ConnectorMapper connectorMapper) {
    this.connectorService = connectorService;
    this.connectorMapper = connectorMapper;
  }

  @POST
  public ConnectorDTO create(@NotNull @Valid ConnectorDTO connectorDTO) {
    return connectorService.create(connectorDTO);
  }
}
