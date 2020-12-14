package io.harness.delegate.beans.connector.gcpconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GcpConnector")
public class GcpConnectorDTO extends ConnectorConfigDTO {
  @Valid GcpConnectorCredentialDTO credential;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (credential.getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      GcpManualDetailsDTO gcpManualDetailsDTO = (GcpManualDetailsDTO) credential.getConfig();
      return gcpManualDetailsDTO;
    }
    return null;
  }
}
