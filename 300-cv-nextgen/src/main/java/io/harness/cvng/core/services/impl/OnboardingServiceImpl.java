package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.OnboardingTaskParameters;
import io.harness.cvng.OnboardingTaskResponse;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.delegate.CVNGTaskType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.services.api.DataSourceConnectivityChecker;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardingServiceImpl implements OnboardingService {
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private NextGenService nextGenService;
  @Inject private Map<DataSourceType, DataSourceConnectivityChecker> dataSourceTypeToServiceMapBinder;
  @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public OnboardingResponseDTO getOnboardingResponse(String accountId, OnboardingRequestDTO onboardingRequestDTO) {
    ConnectorInfoDTO connectorInfoDTO = getConnectorConfigDTO(accountId, onboardingRequestDTO.getConnectorIdentifier(),
        onboardingRequestDTO.getOrgIdentifier(), onboardingRequestDTO.getProjectIdentifier());
    onboardingRequestDTO.getDataCollectionRequest().setConnectorInfoDTO(connectorInfoDTO);
    onboardingRequestDTO.getDataCollectionRequest().setTracingId(onboardingRequestDTO.getTracingId());
    BaseNGAccess ngAccess = BaseNGAccess.builder()
                                .accountIdentifier(accountId)
                                .orgIdentifier(onboardingRequestDTO.getOrgIdentifier())
                                .projectIdentifier(onboardingRequestDTO.getProjectIdentifier())
                                .build();
    OnboardingTaskParameters onboardingTaskParameters =
        OnboardingTaskParameters.builder()
            .accountId(accountId)
            .dataCollectionRequest(onboardingRequestDTO.getDataCollectionRequest())
            .encryptedDataDetails(
                getEncryptionDetails(onboardingRequestDTO.getDataCollectionRequest().getConnectorConfigDTO(), ngAccess))
            .build();
    DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountId)
            .taskType(CVNGTaskType.ONBOARDING_DATA_COLLECTION_RESULT.name())
            .taskParameters(onboardingTaskParameters)
            .executionTimeout(Duration.ofMillis(TaskData.DEFAULT_SYNC_CALL_TIMEOUT))
            .taskSetupAbstraction("orgIdentifier", ngAccess.getOrgIdentifier())
            .taskSetupAbstraction("projectIdentifier", ngAccess.getProjectIdentifier())
            .build();
    OnboardingTaskResponse delegateResponseData =
        (OnboardingTaskResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    //	  String response =
    //        verificationManagerService.getDataCollectionResponse(accountId, onboardingRequestDTO.getOrgIdentifier(),
    //            onboardingRequestDTO.getProjectIdentifier(), onboardingRequestDTO.getDataCollectionRequest());
    log.debug("response", delegateResponseData);
    return OnboardingResponseDTO.builder()
        .accountId(accountId)
        .connectorIdentifier(onboardingRequestDTO.getConnectorIdentifier())
        .orgIdentifier(onboardingRequestDTO.getOrgIdentifier())
        .projectIdentifier(onboardingRequestDTO.getProjectIdentifier())
        .tracingId(onboardingRequestDTO.getTracingId())
        .result(JsonUtils.asObject(delegateResponseData.getJsonResponse(), Object.class))
        .build();
  }

  @Override
  public void checkConnectivity(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String tracingId, DataSourceType dataSourceType) {
    Preconditions.checkNotNull(dataSourceType);
    Preconditions.checkNotNull(dataSourceTypeToServiceMapBinder.containsKey(dataSourceType));
    dataSourceTypeToServiceMapBinder.get(dataSourceType)
        .checkConnectivity(accountId, orgIdentifier, projectIdentifier, connectorIdentifier, tracingId);
  }

  private ConnectorInfoDTO getConnectorConfigDTO(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    return connectorDTO.get();
  }

  private List<EncryptedDataDetail> getEncryptionDetails(
      @Nonnull ConnectorConfigDTO connectorConfigDTO, @Nonnull NGAccess ngAccess) {
    List<DecryptableEntity> decryptableEntities = connectorConfigDTO.getDecryptableEntities();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    if (isEmpty(decryptableEntities)) {
      return encryptedDataDetails;
    }

    decryptableEntities.forEach(decryptableEntity
        -> encryptedDataDetails.addAll(secretManagerClientService.getEncryptionDetails(ngAccess, decryptableEntity)));
    return encryptedDataDetails;
  }
}
