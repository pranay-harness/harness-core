package io.harness.connector;

import io.harness.ng.core.dto.ErrorDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorValidationResult {
  ConnectivityStatus status;
  List<ErrorDetail> errors;
  String errorSummary;
  long testedAt;
  String delegateId;
}