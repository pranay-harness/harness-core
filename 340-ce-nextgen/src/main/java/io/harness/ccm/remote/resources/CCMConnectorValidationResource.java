package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.connectors.CEAWSConnectorValidator;
import io.harness.ccm.connectors.CEAzureConnectorValidator;
import io.harness.ccm.connectors.CEGcpConnectorValidator;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Api("testconnection")
@Path("/testconnection")
@Produces("application/json")
@NextGenManagerAuth
@Slf4j
@Service
@InternalApi
@OwnedBy(CE)
public class CCMConnectorValidationResource {
  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Validate connector", nickname = "validate connector")
  public RestResponse<ConnectorValidationResult> testConnection(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, ConnectorResponseDTO connectorResponseDTO) {
    // Implement validation methods for each connector type
    ConnectorType connectorType = connectorResponseDTO.getConnector().getConnectorType();
    log.info("Connector response dto {}", connectorResponseDTO);
    if (connectorType.equals(ConnectorType.CE_AWS)) {
      return new RestResponse(CEAWSConnectorValidator.validate(connectorResponseDTO, accountId));
    } else if (connectorType.equals(ConnectorType.CE_AZURE)) {
      return new RestResponse(CEAzureConnectorValidator.validate(connectorResponseDTO, accountId));
    } else if (connectorType.equals(ConnectorType.GCP_CLOUD_COST)) {
      return new RestResponse(CEGcpConnectorValidator.validate(connectorResponseDTO, accountId));
    }
    return new RestResponse<>();
  }
}
