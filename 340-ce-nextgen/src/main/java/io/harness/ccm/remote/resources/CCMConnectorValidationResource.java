package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.connectors.AbstractCEConnectorValidator;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ng.core.dto.ResponseDTO;
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
  public ResponseDTO<ConnectorValidationResult> testConnection(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, ConnectorResponseDTO connectorResponseDTO) {
    // Implement validation methods for each connector type
    ConnectorType connectorType = connectorResponseDTO.getConnector().getConnectorType();
    AbstractCEConnectorValidator ceConnectorValidator = io.harness.ccm.connectors.CEConnectorValidatorFactory.getValidator(connectorType);
    if (ceConnectorValidator != null) {
      log.info("Connector response dto {}", connectorResponseDTO);
      return ResponseDTO.newResponse(ceConnectorValidator.validate(connectorResponseDTO, accountId));
    } else {
      return ResponseDTO.newResponse();
    }
  }
}
