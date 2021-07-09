package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.ccmAws.AwsAccountConnectionDetailsHelper;
import io.harness.ccm.commons.entities.AwsAccountConnectionDetail;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.GET;
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
  @Inject AwsAccountConnectionDetailsHelper awsAccountConnectionDetailsHelper;
  @Inject private ConnectorResourceClient connectorResourceClient;

  @GET
  @Path("/azureappclientid")
  @ApiOperation(value = "Get Azure application client Id", nickname = "azureappclientid")
  public ResponseDTO<String> getAzureAppClientId() {
    return ResponseDTO.newResponse(configuration.getCeAzureSetupConfig().getAzureAppClientId());
  }

  @GET
  @Path("/dummy")
  @ApiOperation(value = "Get All Connectors", nickname = "getAllConnectors")
  public ResponseDTO<List<ConnectorResponseDTO>> getDummyConnectorsList() {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    int page = 0;
    int size = 100;
    do {
      response = execute(connectorResourceClient.listConnectors("kmpySmUISimoRrJL6NL73w", null, null, page, size,
              ConnectorFilterPropertiesDTO.builder()
                      .types(Arrays.asList(ConnectorType.CE_AWS))
                      .ccmConnectorFilter(
                              CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
                      .build(),
          false));
      if (response != null && isNotEmpty(response.getContent())) {
        log.info("connectorResourceClient.listConnectors Response:{}", response);
        nextGenConnectorResponses.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));
    return ResponseDTO.newResponse(nextGenConnectorResponses);
  }

  @GET
  @Path("/awsaccountconnectiondetail")
  @ApiOperation(value = "Get Aws account connection details", nickname = "awsaccountconnectiondetail")
  public ResponseDTO<AwsAccountConnectionDetail> getAwsAccountConnectionDetail(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    return ResponseDTO.newResponse(awsAccountConnectionDetailsHelper.getAwsAccountConnectorDetail(accountId));
  }
}
