package io.harness.ceng.apis;

import io.harness.ng.core.dto.ResponseDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Api("/ceng")
@Path("/ceng")
@Produces({"application/json"})
@Consumes({"application/json"})
public class CENextGenServiceResource {
  /**
   * A dummy endpoint just for starter.
   * @return boolean response
   */
  @GET
  @Path("/health")
  @ApiOperation(value = "Get ce microservice status", nickname = "health")
  public ResponseDTO<Boolean> health() {
    return ResponseDTO.newResponse(true);
  }

  // CENG GCP connector helper api calls will appear here
  // like creating a service account tailormade for the gcp cloud connector.
}
