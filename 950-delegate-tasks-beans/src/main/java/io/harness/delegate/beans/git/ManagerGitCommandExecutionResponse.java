package io.harness.delegate.beans.git;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.ConnectorValidationResponseData;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.ManagerResponseData;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data
@Builder
public class ManagerGitCommandExecutionResponse implements ConnectorValidationResponseData {
  private ConnectorValidationResult connectorValidationResult;
}
