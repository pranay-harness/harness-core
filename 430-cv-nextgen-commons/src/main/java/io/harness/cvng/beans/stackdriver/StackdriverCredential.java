package io.harness.cvng.beans.stackdriver;

import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.gcp.helpers.GcpCredentialsHelperService;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
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

  public static StackdriverCredential fromGcpConnector(GcpConnectorDTO gcpConnectorDTO) throws IOException {
    switch (gcpConnectorDTO.getCredential().getGcpCredentialType()) {
      case MANUAL_CREDENTIALS:
        GcpManualDetailsDTO gcpManualDetailsDTO = (GcpManualDetailsDTO) gcpConnectorDTO.getCredential().getConfig();
        String decryptedSecret = new String(gcpManualDetailsDTO.getSecretKeyRef().getDecryptedValue());
        return JsonUtils.asObject(decryptedSecret, StackdriverCredential.class);
      case INHERIT_FROM_DELEGATE:
        GoogleCredential googleCredential = GcpCredentialsHelperService.getApplicationDefaultCredentials();
        return StackdriverCredential.builder()
            .privateKey(googleCredential.getServiceAccountPrivateKey().toString())
            .privateKeyId(googleCredential.getServiceAccountPrivateKeyId())
            .projectId(googleCredential.getServiceAccountProjectId())
            .clientEmail(googleCredential.getServiceAccountUser())
            .clientId(googleCredential.getServiceAccountUser())
            .build();
      default:
        throw new IllegalStateException(
            String.format("Unsupported gcp credential type: %s", gcpConnectorDTO.getCredential().getConfig()));
    }
  }
}
