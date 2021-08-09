package io.harness.ng.core.activityhistory.dto;

import io.harness.connector.ConnectorValidationResult;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("ConnectivityCheckActivityDetail")
public class ConnectivityCheckActivityDetailDTO implements ActivityDetail {
  ConnectorValidationResult connectorValidationResult;
}
