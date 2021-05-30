package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Api("connector")
@Path("/connector")
@Produces({MediaType.APPLICATION_JSON})
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class ConnectorSetup {
  @Inject CENextGenConfiguration configuration;
  @Inject ConnectorResourceClient connectorResourceClient;

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

  @GET
  @Path("/getk8sreferenceconnectors")
  @ApiOperation(value = "Get List of Possible Base K8s Ref Connectors", nickname = "getBaseK8sRefConnectors")
  public ResponseDTO<PageResponse<ConnectorResponseDTO>> getBaseK8sReferenceConnectorsList(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) int size) {
    PageResponse<ConnectorResponseDTO> pagedResponse = execute(connectorResourceClient.listConnectors(accountId, null,
        null, page, size,
        ConnectorFilterPropertiesDTO.builder().types(Arrays.asList(ConnectorType.KUBERNETES_CLUSTER)).build(), false));
    return ResponseDTO.newResponse(pagedResponse);
  }
}
