package io.harness.delegate.beans.artifactory;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactoryTaskResponse implements DelegateResponseData {
  ConnectorValidationResult connectorValidationResult;
  DelegateMetaInfo delegateMetaInfo;
}
