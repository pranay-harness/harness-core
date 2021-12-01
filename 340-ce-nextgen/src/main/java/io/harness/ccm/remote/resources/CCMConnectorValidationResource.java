package io.harness.ccm.remote.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.connectors.AbstractCEConnectorValidator;
import io.harness.ccm.connectors.CEConnectorValidatorFactory;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static io.harness.annotations.dev.HarnessTeam.CE;

@Api("testconnection")
@Path("/testconnection")
@Produces("application/json")
@NextGenManagerAuth
@Slf4j
@Service
@InternalApi
@OwnedBy(CE)
public class CCMConnectorValidationResource {
  @Inject CEConnectorValidatorFactory ceConnectorValidatorFactory;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Validate connector", nickname = "validate connector")
  public ResponseDTO<ConnectorValidationResult> testConnection(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, ConnectorResponseDTO connectorResponseDTO) {
    // Implement validation methods for each connector type
    ConnectorType connectorType = connectorResponseDTO.getConnector().getConnectorType();
    AbstractCEConnectorValidator ceConnectorValidator = ceConnectorValidatorFactory.getValidator(connectorType);
    if (ceConnectorValidator != null) {
      log.info("Connector response dto {}", connectorResponseDTO);
      return ResponseDTO.newResponse(ceConnectorValidator.validate(connectorResponseDTO, accountId));
    } else {
      return ResponseDTO.newResponse();
    }
  }
}
