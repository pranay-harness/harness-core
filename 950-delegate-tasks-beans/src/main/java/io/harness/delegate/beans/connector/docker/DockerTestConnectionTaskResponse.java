package io.harness.delegate.beans.connector.docker;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DockerTestConnectionTaskResponse implements DelegateResponseData {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
