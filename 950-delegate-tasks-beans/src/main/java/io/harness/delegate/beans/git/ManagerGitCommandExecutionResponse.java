package io.harness.delegate.beans.git;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.ManagerResponseData;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Builder
@Data
public class ManagerGitCommandExecutionResponse implements ManagerResponseData {
  private ConnectorValidationResult connectorValidationResult;
}
