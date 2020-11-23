package io.harness.perpetualtask.connector;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.CONNECTOR_IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTask;
import io.harness.connector.apis.client.ConnectorResourceClient;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.RestCallToNGManagerClientUtils;

import software.wings.beans.TaskType;
import software.wings.service.intfc.security.NGSecretService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class ConnectorHeartbeatPerpetualTaskClient implements PerpetualTaskServiceClient {
  private KryoSerializer kryoSerializer;
  private ConnectorResourceClient connectorResourceClient;
  private NGSecretService ngSecretService;

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String accountIdentifier = clientParams.get(ACCOUNT_KEY);
    ConnectorDTO connectorDTO = getConnector(clientParams);
    ByteString connectorConfigBytes = ByteString.copyFrom(kryoSerializer.asBytes(connectorDTO));
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptionDataDetails(connectorDTO, accountIdentifier);
    ByteString encryptedDataDetailsBytes = ByteString.copyFrom(kryoSerializer.asBytes(encryptedDataDetails));
    return ConnectorHeartbeatTaskParams.newBuilder()
        .setAccountIdentifier(accountIdentifier)
        .setConnector(connectorConfigBytes)
        .setEncryptionDetails(encryptedDataDetailsBytes)
        .build();
  }

  private List<EncryptedDataDetail> getEncryptionDataDetails(ConnectorDTO connectorDTO, String accountId) {
    DecryptableEntity decryptableEntity = getDecryptableEntity(connectorDTO);
    if (decryptableEntity == null) {
      return Collections.emptyList();
    }
    ConnectorInfoDTO connectorInfoDTO = connectorDTO.getConnectorInfo();
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountId)
                                       .orgIdentifier(connectorInfoDTO.getOrgIdentifier())
                                       .projectIdentifier(connectorInfoDTO.getProjectIdentifier())
                                       .build();

    return ngSecretService.getEncryptionDetails(basicNGAccessObject, decryptableEntity);
  }

  private DecryptableEntity getDecryptableEntity(ConnectorDTO connectorDTO) {
    if (connectorDTO == null || connectorDTO.getConnectorInfo() == null
        || connectorDTO.getConnectorInfo().getConnectorConfig() == null) {
      throw new UnexpectedException("The connector is null in the heartbeat framework");
    }
    ConnectorConfigDTO connectorConfigDTO = connectorDTO.getConnectorInfo().getConnectorConfig();
    return connectorConfigDTO.getDecryptableEntity();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    Map<String, String> clientParams = clientContext.getClientParams();
    ConnectorDTO connector = getConnector(clientParams);
    List<ExecutionCapability> executionCapabilities =
        connector.getConnectorInfo().fetchRequiredExecutionCapabilitiesForConnector(accountId);
    return DelegateTask.builder()
        .accountId(accountId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.CAPABILITY_VALIDATION.name())
                  .parameters(new Object[] {executionCapabilities})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }

  private ConnectorDTO getConnector(Map<String, String> clientParams) {
    String accountIdentifier = clientParams.get(ACCOUNT_KEY);
    String orgIdentifier = clientParams.get(ORG_KEY);
    String projectIdentifier = clientParams.get(PROJECT_KEY);
    String connectorIdentifier = clientParams.get(CONNECTOR_IDENTIFIER_KEY);
    Optional<ConnectorDTO> connectorDTO = RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.get(connectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier));
    if (!connectorDTO.isPresent()) {
      log.info(
          "{} The connector doesn't exists with the following ids accountId: {}, orgId: {}, projectId:{}, identifier:{}",
          CONNECTOR_HEARTBEAT_LOG_PREFIX, accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      throw new UnexpectedException(String.format("The " + CONNECTOR_STRING + " doesn't exists.", connectorIdentifier,
          accountIdentifier, orgIdentifier, projectIdentifier));
    }
    return connectorDTO.get();
  }
}