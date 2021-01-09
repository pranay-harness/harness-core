package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsValidateTaskResponse implements AwsDelegateTaskResponse {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
