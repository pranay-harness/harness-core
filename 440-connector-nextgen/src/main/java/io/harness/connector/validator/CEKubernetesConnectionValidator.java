package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static software.wings.beans.TaskType.CE_VALIDATE_KUBERNETES_CONFIG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Instant;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class CEKubernetesConnectionValidator extends KubernetesConnectionValidator {
  @Inject @Named("defaultConnectorService") ConnectorService connectorService;

  public static final String CONNECTOR_REF_NOT_EXIST =
      "There does not exist a K8sCluster Cloud Provider connector with connectorRef='%s' ";

  @Override
  public String getTaskType() {
    return CE_VALIDATE_KUBERNETES_CONFIG.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO = (CEKubernetesClusterConfigDTO) connectorDTO;

    Optional<ConnectorConfigDTO> kubernetesConnectorConfigDTO = Optional.empty();
    try {
      kubernetesConnectorConfigDTO = getReferencedConnectorConfig(
          ceKubernetesClusterConfigDTO.getConnectorRef(), accountIdentifier, orgIdentifier, projectIdentifier);
    } catch (IllegalArgumentException ex) {
      if (ex.getMessage().contains("No scope found for string")) {
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errorSummary(ex.getMessage())
            .testedAt(Instant.now().toEpochMilli())
            .build();
      }
      throw ex;
    }

    if (!kubernetesConnectorConfigDTO.isPresent()) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary(String.format(CONNECTOR_REF_NOT_EXIST, ceKubernetesClusterConfigDTO.getConnectorRef()))
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
    // should we pass down the orgIdentifier, projectIdentifier of the referenced (Cloud_Provider connector) or
    // referencing (cloud_Cost connector) connector?
    return super.validate(
        kubernetesConnectorConfigDTO.get(), accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  private Optional<ConnectorConfigDTO> getReferencedConnectorConfig(@NotNull String scopedConnectorIdentifier,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(
        scopedConnectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
    Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());
    return connectorResponseDTO.map(responseDTO -> responseDTO.getConnector().getConnectorConfig());
  }
}
