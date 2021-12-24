package io.harness.delegate.task.gcp.response;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.ConnectorValidationResponseData;
import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GcpValidationTaskResponse implements ConnectorValidationResponseData, GcpResponse {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
