package io.harness.delegate.beans.artifactory;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArtifactoryTaskResponse implements DelegateTaskNotifyResponseData {
  ConnectorValidationResult connectorValidationResult;
  DelegateMetaInfo delegateMetaInfo;
}
