package io.harness.delegate.beans.connector;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConnectorValidationResult {
  boolean valid;
  String errorMessage;
}
