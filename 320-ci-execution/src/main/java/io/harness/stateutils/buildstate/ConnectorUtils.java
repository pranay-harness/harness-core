package io.harness.stateutils.buildstate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.IdentifierRef;
import io.harness.connector.apis.client.ConnectorResourceClient;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ConnectorDetails.ConnectorDetailsBuilder;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ConnectorUtils {
  public static final String CREDENTIALS_CAN_T_BE_EMPTY = "Connector credentials can't be empty";
  private final ConnectorResourceClient connectorResourceClient;
  private final SecretManagerClientService secretManagerClientService;

  @Inject
  public ConnectorUtils(
      ConnectorResourceClient connectorResourceClient, SecretManagerClientService secretManagerClientService) {
    this.connectorResourceClient = connectorResourceClient;
    this.secretManagerClientService = secretManagerClientService;
  }

  public Map<String, ConnectorDetails> getConnectorDetailsMap(NGAccess ngAccess, Set<String> connectorNameSet) {
    Map<String, ConnectorDetails> connectorDetailsMap = new HashMap<>();
    if (isNotEmpty(connectorNameSet)) {
      for (String connectorIdentifier : connectorNameSet) {
        ConnectorDetails connectorDetails = getConnectorDetails(ngAccess, connectorIdentifier);
        connectorDetailsMap.put(connectorDetails.getIdentifier(), connectorDetails);
      }
    }

    return connectorDetailsMap;
  }

  public ConnectorDetails getConnectorDetails(NGAccess ngAccess, String connectorIdentifier) {
    log.info("Getting connector details for connector ref [{}]", connectorIdentifier);
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(connectorIdentifier,
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());

    ConnectorDTO connectorDTO = getConnector(connectorRef);
    List<EncryptedDataDetail> encryptedDataDetails;
    ConnectorType connectorType = connectorDTO.getConnectorInfo().getConnectorType();

    ConnectorDetailsBuilder connectorDetailsBuilder =
        ConnectorDetails.builder()
            .connectorType(connectorType)
            .connectorConfig(connectorDTO.getConnectorInfo().getConnectorConfig())
            .identifier(connectorDTO.getConnectorInfo().getIdentifier())
            .orgIdentifier(connectorDTO.getConnectorInfo().getOrgIdentifier())
            .projectIdentifier(connectorDTO.getConnectorInfo().getProjectIdentifier());

    log.info("Getting encryption details for connector details for connector id:[{}] type:[{}]", connectorIdentifier,
        connectorType);
    switch (connectorType) {
      case DOCKER:
        DockerConnectorDTO dockerConnectorDTO =
            (DockerConnectorDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
        encryptedDataDetails =
            secretManagerClientService.getEncryptionDetails(ngAccess, dockerConnectorDTO.getAuth().getCredentials());
        if (isEmpty(encryptedDataDetails)) {
          throw new InvalidArgumentsException(CREDENTIALS_CAN_T_BE_EMPTY, WingsException.USER);
        }
        return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();

      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO kubernetesClusterConfigDTO =
            (KubernetesClusterConfigDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
        KubernetesCredentialDTO config = kubernetesClusterConfigDTO.getCredential();
        if (config.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
          KubernetesClusterDetailsDTO kubernetesCredentialSpecDTO = (KubernetesClusterDetailsDTO) config.getConfig();
          KubernetesAuthCredentialDTO kubernetesAuthCredentialDTO =
              kubernetesCredentialSpecDTO.getAuth().getCredentials();
          encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, kubernetesAuthCredentialDTO);
          if (isEmpty(encryptedDataDetails)) {
            throw new InvalidArgumentsException(CREDENTIALS_CAN_T_BE_EMPTY, WingsException.USER);
          }
          return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
        } else {
          return connectorDetailsBuilder.build();
        }

      case GIT:
        GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
        GitAuthenticationDTO gitAuth = gitConfigDTO.getGitAuth();
        encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, gitAuth);

        if (isEmpty(encryptedDataDetails)) {
          throw new InvalidArgumentsException(CREDENTIALS_CAN_T_BE_EMPTY, WingsException.USER);
        }
        return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();

      default:
        throw new InvalidArgumentsException("Unexpected connector type=[]: " + connectorType);
    }
  }

  private ConnectorDTO getConnector(IdentifierRef connectorRef) {
    Optional<ConnectorDTO> connectorDTO;

    try {
      log.info("Fetching connector details for connector id:[{}] acc:[{}] project:[{}] org:[{}]",
          connectorRef.getIdentifier(), connectorRef.getAccountIdentifier(), connectorRef.getProjectIdentifier(),
          connectorRef.getOrgIdentifier());

      connectorDTO =
          SafeHttpCall
              .execute(connectorResourceClient.get(connectorRef.getIdentifier(), connectorRef.getAccountIdentifier(),
                  connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier()))
              .getData();

    } catch (Exception e) {
      throw new UnexpectedException(String.format("Unable to get connector information : [%s] with scope: [%s]",
                                        connectorRef.getIdentifier(), connectorRef.getScope()),
          e);
    }

    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(String.format("Connector not found for identifier : [%s] with scope: [%s]",
                                            connectorRef.getIdentifier(), connectorRef.getScope()),
          WingsException.USER);
    }
    return connectorDTO.get();
  }
}
