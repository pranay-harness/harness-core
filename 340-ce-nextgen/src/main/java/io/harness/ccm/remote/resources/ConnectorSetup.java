package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Api("/connector")
@Path("/connector")
@Produces({MediaType.APPLICATION_JSON})
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class ConnectorSetup {
  @Inject CENextGenConfiguration configuration;

  @POST
  @Path("/getceawstemplateurl")
  @ApiOperation(value = "Get CE Aws Connector Template URL Environment Wise", nickname = "getCEAwsTemplate")
  public ResponseDTO<String> getCEAwsTemplate(
      @QueryParam(NGCommonEntityConstants.IS_EVENTS_ENABLED) Boolean eventsEnabled,
      @QueryParam(NGCommonEntityConstants.IS_CUR_ENABLED) Boolean curEnabled,
      @QueryParam(NGCommonEntityConstants.IS_OPTIMIZATION_ENABLED) Boolean optimizationEnabled) {
    final String templateURL = configuration.getAwsConnectorTemplate();
    return ResponseDTO.newResponse(templateURL);
  }
}
