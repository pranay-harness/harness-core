package io.harness.delegate.task.gcp.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GcpValidationTaskResponse implements GcpResponse {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
