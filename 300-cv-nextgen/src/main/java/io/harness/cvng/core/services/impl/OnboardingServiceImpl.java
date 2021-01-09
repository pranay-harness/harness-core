package io.harness.cvng.core.services.impl;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.serializer.JsonUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private NextGenService nextGenService;
  @Override
  public OnboardingResponseDTO getOnboardingResponse(String accountId, OnboardingRequestDTO onboardingRequestDTO) {
    ConnectorInfoDTO connectorInfoDTO = getConnectorConfigDTO(accountId, onboardingRequestDTO.getConnectorIdentifier(),
        onboardingRequestDTO.getOrgIdentifier(), onboardingRequestDTO.getProjectIdentifier());
    onboardingRequestDTO.getDataCollectionRequest().setConnectorInfoDTO(connectorInfoDTO);
    onboardingRequestDTO.getDataCollectionRequest().setTracingId(onboardingRequestDTO.getTracingId());
    String response =
        verificationManagerService.getDataCollectionResponse(accountId, onboardingRequestDTO.getOrgIdentifier(),
            onboardingRequestDTO.getProjectIdentifier(), onboardingRequestDTO.getDataCollectionRequest());
    log.info(response);
    return OnboardingResponseDTO.builder()
        .accountId(accountId)
        .connectorIdentifier(onboardingRequestDTO.getConnectorIdentifier())
        .orgIdentifier(onboardingRequestDTO.getOrgIdentifier())
        .projectIdentifier(onboardingRequestDTO.getProjectIdentifier())
        .tracingId(onboardingRequestDTO.getTracingId())
        .result(JsonUtils.asObject(response, Object.class))
        .build();
  }

  private ConnectorInfoDTO getConnectorConfigDTO(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    return connectorDTO.get();
  }
}
