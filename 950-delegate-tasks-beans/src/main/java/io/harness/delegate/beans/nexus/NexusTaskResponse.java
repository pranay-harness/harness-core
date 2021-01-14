package io.harness.delegate.beans.nexus;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NexusTaskResponse implements DelegateTaskNotifyResponseData {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
