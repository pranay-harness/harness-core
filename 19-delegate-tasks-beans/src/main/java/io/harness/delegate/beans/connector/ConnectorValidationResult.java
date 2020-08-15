package io.harness.delegate.beans.connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorValidationResult {
  boolean valid;
  String errorMessage;
  long testedAt;
}
