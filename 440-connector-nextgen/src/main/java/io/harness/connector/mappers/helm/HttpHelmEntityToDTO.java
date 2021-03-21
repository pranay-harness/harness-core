package io.harness.connector.mappers.helm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.delegate.beans.connector.helm.HttpHelmAuthType.ANONYMOUS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.helm.HttpHelmConnector;
import io.harness.connector.entities.embedded.helm.HttpHelmUsernamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class HttpHelmEntityToDTO implements ConnectorEntityToDTOMapper<HttpHelmConnectorDTO, HttpHelmConnector> {
  @Override
  public HttpHelmConnectorDTO createConnectorDTO(HttpHelmConnector connector) {
    return HttpHelmConnectorDTO.builder()
        .helmRepoUrl(connector.getUrl())
        .auth(createHttpHelmAuthDTO(connector))
        .build();
  }

  private HttpHelmAuthenticationDTO createHttpHelmAuthDTO(HttpHelmConnector connector) {
    if (connector.getHttpHelmAuthentication() == null || connector.getAuthType() == ANONYMOUS) {
      return HttpHelmAuthenticationDTO.builder().authType(ANONYMOUS).build();
    }

    HttpHelmUsernamePasswordAuthentication httpHelmCredentials =
        (HttpHelmUsernamePasswordAuthentication) connector.getHttpHelmAuthentication();
    HttpHelmUsernamePasswordDTO httpHelmUserNamePasswordDTO =
        HttpHelmUsernamePasswordDTO.builder()
            .username(httpHelmCredentials.getUsername())
            .usernameRef(SecretRefHelper.createSecretRef(httpHelmCredentials.getUsernameRef()))
            .passwordRef(SecretRefHelper.createSecretRef(httpHelmCredentials.getPasswordRef()))
            .build();
    return HttpHelmAuthenticationDTO.builder()
        .authType(connector.getAuthType())
        .credentials(httpHelmUserNamePasswordDTO)
        .build();
  }
}
