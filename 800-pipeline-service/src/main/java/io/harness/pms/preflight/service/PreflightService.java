package io.harness.pms.preflight.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.entity.PreFlightEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PreflightService {
  void updateStatus(String id, PreFlightStatus status, PreFlightEntityErrorInfo errorInfo);

  List<ConnectorCheckResponse> updateConnectorCheckResponses(String accountId, String orgId, String projectId,
      String preflightEntityId, Map<String, Object> fqnToObjectMapMergedYaml, List<EntityDetail> connectorUsages);

  PreFlightEntity saveEmptyPreflightEntity(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineYaml, List<EntityDetail> entityDetails);

  PreFlightDTO getPreflightCheckResponse(String preflightCheckId);

  String startPreflightCheck(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml)
      throws IOException;
}
