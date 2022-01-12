/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.validator;

import static software.wings.beans.TaskType.CE_VALIDATE_KUBERNETES_CONFIG;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.scmValidators.AbstractKubernetesConnectorValidator;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.CEKubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEKubernetesConnectionValidator extends AbstractKubernetesConnectorValidator {
  @Inject @Named("defaultConnectorService") ConnectorService connectorService;

  public static final String CONNECTOR_REF_NOT_EXIST =
      "There does not exist a K8sCluster Cloud Provider connector with connectorRef='%s' ";

  @Override
  public String getTaskType() {
    return CE_VALIDATE_KUBERNETES_CONFIG.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO kubernetesClusterConfig, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    DelegateResponseData responseData;

    try {
      responseData = super.validateConnector(
          kubernetesClusterConfig, accountIdentifier, orgIdentifier, projectIdentifier, identifier);

      KubernetesConnectionTaskResponse taskResponse = (KubernetesConnectionTaskResponse) responseData;
      return taskResponse.getConnectorValidationResult();
    } catch (IllegalArgumentException ex) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary(ex.getMessage())
          .testedAt(Instant.now().toEpochMilli())
          .build();
    } catch (Exception ex) {
      log.error("Unknown error found while validating ccm k8s connector", ex);

      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary(ex.getMessage())
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO = (CEKubernetesClusterConfigDTO) connectorConfig;

    final IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(
        ceKubernetesClusterConfigDTO.getConnectorRef(), accountIdentifier, orgIdentifier, projectIdentifier);

    Optional<ConnectorConfigDTO> kubernetesConnectorConfigDTO = getReferencedConnectorConfig(connectorRef);
    if (!kubernetesConnectorConfigDTO.isPresent()) {
      throw new IllegalArgumentException(
          String.format(CONNECTOR_REF_NOT_EXIST, ceKubernetesClusterConfigDTO.getConnectorRef()));
    }
    KubernetesClusterConfigDTO kubernetesClusterConfig =
        (KubernetesClusterConfigDTO) kubernetesConnectorConfigDTO.get();

    List<EncryptedDataDetail> encryptedDataDetailList = super.fetchEncryptionDetailsList(kubernetesClusterConfig,
        accountIdentifier, connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier());

    final List<CEFeatures> featuresEnabled = ceKubernetesClusterConfigDTO.getFeaturesEnabled();
    return CEKubernetesConnectionTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptedDataDetailList)
        .featuresEnabled(featuresEnabled)
        .build();
  }

  private Optional<ConnectorConfigDTO> getReferencedConnectorConfig(final IdentifierRef connectorRef) {
    Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());
    return connectorResponseDTO.map(responseDTO -> responseDTO.getConnector().getConnectorConfig());
  }
}
