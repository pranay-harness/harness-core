package io.harness.connector;

import io.harness.ConnectorConstants;
import io.harness.ng.core.dto.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "This has validation details for the Connector defined in Harness.")
public class ConnectorValidationResult {
  @Schema(description = ConnectorConstants.STATUS) ConnectivityStatus status;
  @Schema(description = ConnectorConstants.ERRORS) List<ErrorDetail> errors;
  @Schema(description = ConnectorConstants.ERROR_SUMMARY) String errorSummary;
  @Schema(description = ConnectorConstants.TESTED_AT) long testedAt;
  @Schema(description = ConnectorConstants.DELEGATE_ID) String delegateId;
}