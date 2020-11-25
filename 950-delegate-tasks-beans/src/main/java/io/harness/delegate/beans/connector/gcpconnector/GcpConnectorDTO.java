package io.harness.delegate.beans.connector.gcpconnector;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GcpConnector")
public class GcpConnectorDTO extends ConnectorConfigDTO implements ExecutionCapabilityDemander {
  @Valid GcpConnectorCredentialDTO credential;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (credential.getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      GcpManualDetailsDTO gcpManualDetailsDTO = (GcpManualDetailsDTO) credential.getConfig();
      return gcpManualDetailsDTO;
    }
    return null;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    final String GCS_URL = "https://storage.cloud.google.com/";
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(GCS_URL));
  }
}
