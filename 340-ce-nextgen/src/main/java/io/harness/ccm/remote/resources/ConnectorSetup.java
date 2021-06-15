package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.ccmAws.AwsAccountConnectionDetailsHelper;
import io.harness.ccm.commons.entities.AwsAccountConnectionDetail;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

  @GET
  @Path("/azureappclientid")
  @ApiOperation(value = "Get Azure application client Id", nickname = "azureappclientid")
  public ResponseDTO<String> getAzureAppClientId() {
    return ResponseDTO.newResponse(configuration.getCeAzureSetupConfig().getAzureAppClientId());
  }

  @GET
  @Path("/awsaccountconnectiondetail")
  @ApiOperation(value = "Get Aws account connection details", nickname = "awsaccountconnectiondetail")
  public ResponseDTO<AwsAccountConnectionDetail> getAwsAccountConnectionDetail(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    return ResponseDTO.newResponse(awsAccountConnectionDetailsHelper.getAwsAccountConnectorDetail(accountId));
  }
}
