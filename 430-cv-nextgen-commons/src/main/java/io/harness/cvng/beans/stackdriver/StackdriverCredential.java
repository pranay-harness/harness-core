package io.harness.cvng.beans.stackdriver;

import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackdriverCredential {
  @JsonProperty("client_email") private String clientEmail;
  @JsonProperty("client_id") private String clientId;
  @JsonProperty("private_key") private String privateKey;
  @JsonProperty("private_key_id") private String privateKeyId;
  @JsonProperty("project_id") private String projectId;

  public static StackdriverCredential fromGcpConnector(GcpConnectorDTO gcpConnectorDTO) {
    if (gcpConnectorDTO.getCredential().getGcpCredentialType() != GcpCredentialType.MANUAL_CREDENTIALS) {
      return null;
    }
    GcpManualDetailsDTO gcpManualDetailsDTO = (GcpManualDetailsDTO) gcpConnectorDTO.getCredential().getConfig();
    String decryptedSecret = new String(gcpManualDetailsDTO.getSecretKeyRef().getDecryptedValue());
    return JsonUtils.asObject(decryptedSecret, StackdriverCredential.class);
  }
}
