package io.harness.ceng.remote.resources;

import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("/ceng/api")
@Path("/ceng/api")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@Slf4j
public class CENextGenServiceResource {
  /**
   * A dummy endpoint just for starter. Will be removed when other APIs are populated here
   * @return boolean response
   */

  //  @Inject ConnectorResourceClient connectorResourceClient;
  @GET
  @ApiOperation(value = "Get ce microservice base api", nickname = "test")
  public ResponseDTO<Boolean> test() {
    //  ResponseDTO<Optional<io.harness.connector.ConnectorDTO>> response =
    //          connectorResourceClient.get("aws0", "kmpySmUISimoRrJL6NL73w", null, null).execute().body();
    //  log.info("{}", response.getData().get().getConnectorInfo());
    return ResponseDTO.newResponse(true);
  }
  // CENG GCP connector helper api calls will appear here
  // like creating a service account tailormade for the gcp cloud connector.
}
