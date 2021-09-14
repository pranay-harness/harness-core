/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.validator;

import static software.wings.beans.TaskType.CE_VALIDATE_KUBERNETES_CONFIG;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
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

    final List<CEFeatures> featuresEnabled = ceKubernetesClusterConfigDTO.getFeaturesEnabled();

    if (featuresEnabled.contains(CEFeatures.VISIBILITY)) {
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

    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
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
