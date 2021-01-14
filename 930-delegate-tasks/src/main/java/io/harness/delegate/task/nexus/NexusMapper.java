package io.harness.delegate.task.nexus;

import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefData;
import io.harness.nexus.NexusRequest;

import com.google.inject.Singleton;

@Singleton
public class NexusMapper {
  public NexusRequest toNexusRequest(NexusConnectorDTO nexusConnectorDTO) {
    final NexusRequest.NexusRequestBuilder nexusRequestBuilder =
        NexusRequest.builder()
            .nexusUrl(nexusConnectorDTO.getNexusServerUrl())
            // Setting as false since we are not taking this in yaml as of now.
            .isCertValidationRequired(false);
    final NexusAuthType authType = nexusConnectorDTO.getAuth().getAuthType();

    if (authType == NexusAuthType.NO_AUTH) {
      return nexusRequestBuilder.hasCredentials(false).build();
    }
    final NexusUsernamePasswordAuthDTO credentials =
        (NexusUsernamePasswordAuthDTO) nexusConnectorDTO.getAuth().getCredentials();
    final SecretRefData passwordRef = credentials.getPasswordRef();
    final String username = credentials.getUsername();
    return nexusRequestBuilder.hasCredentials(true)
        .username(username)
        .password(passwordRef.getDecryptedValue())
        .build();
  }
}
