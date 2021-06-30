package io.harness.ccm.eventframework;

import static java.lang.String.format;

import io.harness.ccm.K8sEventCollectionBundle;
import io.harness.ccm.cluster.NGClusterRecordHandler;
import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.perpetualtask.K8sWatchTaskResourceClient;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
public class EntityChangeHandler {
  @Inject NGClusterRecordHandler clusterRecordHandler;
  @Inject ConnectorResourceClient connectorResourceClient;
  @Inject K8sWatchTaskResourceClient k8sWatchTaskResourceClient;

  public void handleCreateEvent(EntityChangeDTO entityChangeDTO, String connectorEntityType) {
    // Create Events of K8s Base (CD) connectors can be safely ignored
    if (ConnectorType.CE_KUBERNETES_CLUSTER.getDisplayName().equals(connectorEntityType)) {
      handleCEK8sCreate(entityChangeDTO);
    }
  }

  public void handleUpdateEvent(EntityChangeDTO entityChangeDTO, String connectorEntityType) {
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    String k8sConnectorIdentifier = entityChangeDTO.getIdentifier().getValue();

    if (ConnectorType.KUBERNETES_CLUSTER.getDisplayName().equals(connectorEntityType)) {
      ClusterRecord clusterRecordFromK8sBaseConnector =
          clusterRecordHandler.getClusterRecordFromK8sBaseConnector(accountIdentifier, k8sConnectorIdentifier);
      // K8s Cluster Event is Relevant to CE/ CD Connector has CE Enabled
      if (clusterRecordFromK8sBaseConnector != null) {
        String perpetualTaskId = clusterRecordFromK8sBaseConnector.getPerpetualTaskId();
        try {
          resetPerpetualTask(clusterRecordFromK8sBaseConnector, perpetualTaskId);
        } catch (IOException e) {
          log.error("Exception Resetting Perpetual Task for Cluster Record: {}", clusterRecordFromK8sBaseConnector);
        }
      }
    }
    if (ConnectorType.CE_KUBERNETES_CLUSTER.getDisplayName().equals(connectorEntityType)) {
      handleCEK8sUpdate(entityChangeDTO);
    }
  }

  public void handleDeleteEvent(EntityChangeDTO entityChangeDTO, String connectorEntityType) {
    // Deleting base K8s Connector should not be allowed when referenced.
    if (ConnectorType.CE_KUBERNETES_CLUSTER.getDisplayName().equals(connectorEntityType)) {
      String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
      String ceK8sConnectorIdentifier = entityChangeDTO.getIdentifier().getValue();

      ConnectorInfoDTO ceK8sConnectorInfoDTO = getConnectorConfigDTO(entityChangeDTO);
      ConnectorConfigDTO ceK8sConnectorConfigDTO = ceK8sConnectorInfoDTO.getConnectorConfig();
      CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO =
          (CEKubernetesClusterConfigDTO) ceK8sConnectorConfigDTO;
      if (isVisibilityFeatureEnabled(ceKubernetesClusterConfigDTO)) {
        ClusterRecord clusterRecord =
            clusterRecordHandler.getClusterRecord(accountIdentifier, ceK8sConnectorIdentifier);
        clusterRecordAndPerpetualTaskCleanup(clusterRecord, accountIdentifier, ceK8sConnectorIdentifier);
      }
    }
  }

  public void handleCEK8sCreate(EntityChangeDTO entityChangeDTO) {
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    String ceK8sConnectorIdentifier = entityChangeDTO.getIdentifier().getValue();

    ConnectorInfoDTO ceK8sConnectorInfoDTO = getConnectorConfigDTO(entityChangeDTO);
    ConnectorConfigDTO ceK8sConnectorConfigDTO = ceK8sConnectorInfoDTO.getConnectorConfig();
    CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO = (CEKubernetesClusterConfigDTO) ceK8sConnectorConfigDTO;
    if (isVisibilityFeatureEnabled(ceKubernetesClusterConfigDTO)) {
      String k8sBaseConnectorRef = ceKubernetesClusterConfigDTO.getConnectorRef();
      onboardNewCEK8sConnector(getClusterRecord(
          accountIdentifier, ceK8sConnectorIdentifier, ceK8sConnectorInfoDTO.getName(), k8sBaseConnectorRef));
    }
  }

  private void onboardNewCEK8sConnector(ClusterRecord clusterRecord) {
    String taskId;
    try {
      clusterRecord = clusterRecordHandler.handleNewCEK8sConnectorCreate(clusterRecord);
      taskId = createPerpetualTask(clusterRecord);
      clusterRecordHandler.attachPerpetualTask(clusterRecord, taskId);
    } catch (IOException e) {
      log.error("Exception Creating Perpetual Task for Cluster Record: {}", clusterRecord);
    }
  }

  private void clusterRecordAndPerpetualTaskCleanup(
      ClusterRecord clusterRecord, String accountIdentifier, String ceK8sConnectorIdentifier) {
    String perpetualTaskId = clusterRecord.getPerpetualTaskId();
    // Delete the Perpetual Task
    try {
      deletePerpetualTask(clusterRecord, perpetualTaskId);
    } catch (IOException e) {
      log.error("Exception Deleting Perpetual Task for CLuster Record: {}", clusterRecord);
    }
    clusterRecordHandler.deleteClusterRecord(accountIdentifier, ceK8sConnectorIdentifier);
  }

  public void handleCEK8sUpdate(EntityChangeDTO entityChangeDTO) {
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    String ceK8sConnectorIdentifier = entityChangeDTO.getIdentifier().getValue();

    ConnectorInfoDTO ceK8sConnectorInfoDTO = getConnectorConfigDTO(entityChangeDTO);
    CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO =
        (CEKubernetesClusterConfigDTO) ceK8sConnectorInfoDTO.getConnectorConfig();
    ClusterRecord clusterRecord;
    // Get Cluster Record from DB
    clusterRecord = clusterRecordHandler.getClusterRecord(accountIdentifier, ceK8sConnectorIdentifier);
    boolean visibilityFeatureEnabled = isVisibilityFeatureEnabled(ceKubernetesClusterConfigDTO);

    // If Billing was enabled Previously and Disabled in the Update, we need to clean up PT, CR
    if (!visibilityFeatureEnabled && clusterRecord != null) {
      clusterRecordAndPerpetualTaskCleanup(clusterRecord, accountIdentifier, ceK8sConnectorIdentifier);
    }

    if (visibilityFeatureEnabled) {
      if (clusterRecord == null) {
        /* If Billing was enabled as a part of connector Update (Cluster record didn't exist previously)
        - Then we create Cluster Record and Assign a Perpetual Task */
        String k8sBaseConnectorRef = ceKubernetesClusterConfigDTO.getConnectorRef();
        onboardNewCEK8sConnector(getClusterRecord(
            accountIdentifier, ceK8sConnectorIdentifier, ceK8sConnectorInfoDTO.getName(), k8sBaseConnectorRef));
      } else {
        // Only fields that can change on a CE K8s Entity Update (Name, Connector Ref)
        clusterRecord.setClusterName(ceK8sConnectorInfoDTO.getName());
        clusterRecord.setK8sBaseConnectorRefIdentifier(ceKubernetesClusterConfigDTO.getConnectorRef());
        // find all the existing perpetual Tasks for these clusters
        String perpetualTaskId = clusterRecord.getPerpetualTaskId();
        // Reset the Perpetual Task
        try {
          resetPerpetualTask(clusterRecord, perpetualTaskId);
        } catch (IOException e) {
          log.error("Exception Resetting Perpetual Task for CLuster Record: {}", clusterRecord);
        }
      }
    }
  }

  private String createPerpetualTask(ClusterRecord clusterRecord) throws IOException {
    Response<ResponseDTO<String>> createResponse =
        k8sWatchTaskResourceClient
            .create(clusterRecord.getAccountId(),
                K8sEventCollectionBundle.builder()
                    .cloudProviderId(clusterRecord.getCeK8sConnectorIdentifier())
                    .clusterId(clusterRecord.getUuid())
                    .clusterName(clusterRecord.getClusterName())
                    .connectorIdentifier(clusterRecord.getCeK8sConnectorIdentifier())
                    .build())
            .execute();
    return createResponse.body().getData();
  }

  private Boolean resetPerpetualTask(ClusterRecord clusterRecord, String taskId) throws IOException {
    Response<ResponseDTO<Boolean>> createResponse =
        k8sWatchTaskResourceClient
            .reset(clusterRecord.getAccountId(), taskId,
                K8sEventCollectionBundle.builder()
                    .cloudProviderId(clusterRecord.getCeK8sConnectorIdentifier())
                    .clusterId(clusterRecord.getUuid())
                    .clusterName(clusterRecord.getClusterName())
                    .connectorIdentifier(clusterRecord.getCeK8sConnectorIdentifier())
                    .build())
            .execute();
    return createResponse.body().getData();
  }

  Boolean deletePerpetualTask(ClusterRecord clusterRecord, String taskId) throws IOException {
    Response<ResponseDTO<Boolean>> createResponse =
        k8sWatchTaskResourceClient.delete(clusterRecord.getAccountId(), taskId).execute();
    return createResponse.body().getData();
  }

  private ClusterRecord getClusterRecord(
      String accountIdentifier, String ceK8sConnectorIdentifier, String clusterName, String k8sBaseConnectorRef) {
    return ClusterRecord.builder()
        .accountId(accountIdentifier)
        .ceK8sConnectorIdentifier(ceK8sConnectorIdentifier)
        .k8sBaseConnectorRefIdentifier(k8sBaseConnectorRef)
        .clusterName(clusterName)
        .build();
  }

  public ConnectorInfoDTO getConnectorConfigDTO(EntityChangeDTO entityChangeDTO) {
    ConnectorInfoDTO connectorInfoDTO;
    String connectorIdentifierRef = null;
    try {
      connectorIdentifierRef = entityChangeDTO.getIdentifier().getValue();
      Optional<ConnectorDTO> connectorDTO = NGRestUtils.getResponse(
          connectorResourceClient.get(connectorIdentifierRef, entityChangeDTO.getAccountIdentifier().getValue(),
              entityChangeDTO.getOrgIdentifier().getValue(), entityChangeDTO.getProjectIdentifier().getValue()));

      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(
            String.format("Connector not found for identifier : [%s]", connectorIdentifierRef));
      }

      connectorInfoDTO = connectorDTO.get().getConnectorInfo();
    } catch (Exception e) {
      throw new NotFoundException(
          format("Error while getting connector information : [%s]", connectorIdentifierRef), e);
    }
    return connectorInfoDTO;
  }

  private boolean isVisibilityFeatureEnabled(CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO) {
    List<CEFeatures> featuresEnabled = ceKubernetesClusterConfigDTO.getFeaturesEnabled();
    return featuresEnabled.contains(CEFeatures.VISIBILITY);
  }
}
