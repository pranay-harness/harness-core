package io.harness.delegate.beans.connector.helm;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpHelmConnectivityTaskResponse implements DelegateTaskNotifyResponseData {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
