package io.harness.delegate.beans.connector.awsconnector;

import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("AwsConnector")
public class AwsConnectorDTO extends ConnectorConfigDTO implements ExecutionCapabilityDemander {
  @Valid @NotNull AwsCredentialDTO credential;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (credential.getAwsCredentialType() == MANUAL_CREDENTIALS) {
      AwsManualConfigSpecDTO awsManualCredentials = (AwsManualConfigSpecDTO) credential.getConfig();
      return awsManualCredentials;
    }
    return null;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    final String AWS_URL = "https://aws.amazon.com/";
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL));
  }
}
