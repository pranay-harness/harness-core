package io.harness.pms.preflight.mapper;

import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.connector.ConnectorWrapperResponse;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.inputset.PipelineInputResponse;
import io.harness.pms.preflight.inputset.PipelineWrapperResponse;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PreFlightMapper {
  public PreFlightEntity toEmptyEntity(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String pipelineYaml) {
    return PreFlightEntity.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .pipelineIdentifier(pipelineIdentifier)
        .pipelineYaml(pipelineYaml)
        .pipelineInputResponse(Collections.emptyList())
        .connectorCheckResponse(Collections.emptyList())
        .build();
  }

  public PreFlightDTO toPreFlightDTO(PreFlightEntity entity) {
    PreFlightStatus pipelineInputStatus = getPipelineInputStatus(entity.getPipelineInputResponse());
    PipelineWrapperResponse pipelineWrapperResponse = PipelineWrapperResponse.builder()
                                                          .pipelineInputResponse(entity.getPipelineInputResponse())
                                                          .status(pipelineInputStatus)
                                                          .build();

    PreFlightStatus connectorCheckStatus = getConnectorCheckStatus(entity.getConnectorCheckResponse());
    ConnectorWrapperResponse connectorWrapperResponse = ConnectorWrapperResponse.builder()
                                                            .checkResponses(entity.getConnectorCheckResponse())
                                                            .status(connectorCheckStatus)
                                                            .build();
    return PreFlightDTO.builder()
        .pipelineInputWrapperResponse(pipelineWrapperResponse)
        .connectorWrapperResponse(connectorWrapperResponse)
        .status(getOverallStatus(pipelineInputStatus, connectorCheckStatus))
        .build();
  }

  private PreFlightStatus getPipelineInputStatus(List<PipelineInputResponse> pipelineInputResponse) {
    for (PipelineInputResponse response : pipelineInputResponse) {
      if (!response.isSuccess()) {
        return PreFlightStatus.FAILURE;
      }
    }
    return PreFlightStatus.SUCCESS;
  }

  private PreFlightStatus getConnectorCheckStatus(List<ConnectorCheckResponse> connectorCheckResponse) {
    for (ConnectorCheckResponse response : connectorCheckResponse) {
      PreFlightStatus status = response.getStatus();
      if (status != PreFlightStatus.SUCCESS) {
        return status;
      }
    }
    return PreFlightStatus.SUCCESS;
  }

  private PreFlightStatus getOverallStatus(PreFlightStatus pipelineInputStatus, PreFlightStatus connectorStatus) {
    if (pipelineInputStatus == PreFlightStatus.FAILURE || connectorStatus == PreFlightStatus.FAILURE) {
      return PreFlightStatus.FAILURE;
    }
    return connectorStatus;
  }
}
