package io.harness.delegate.beans.nexus;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.ConnectorValidationResponseData;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NexusTaskResponse implements ConnectorValidationResponseData, DelegateTaskNotifyResponseData {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
