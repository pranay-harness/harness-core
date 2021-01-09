package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesConnectionTaskResponse implements DelegateResponseData {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
