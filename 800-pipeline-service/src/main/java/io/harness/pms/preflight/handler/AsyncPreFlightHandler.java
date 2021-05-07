package io.harness.pms.preflight.handler;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNUtils;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.PreflightCommonUtils;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.service.PreflightService;
import io.harness.pms.yaml.PmsYamlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Builder
@Slf4j
public class AsyncPreFlightHandler implements Runnable {
  private final PreFlightEntity entity;
  private final List<EntityDetail> entityDetails;
  private final PreflightService preflightService;

  @Override
  public void run() {
    try (AutoLogContext ignore = entity.autoLogContext()) {
      log.info("Handling preflight check with id " + entity.getUuid() + " for pipeline with id "
          + entity.getPipelineIdentifier());
      Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();
      try {
        Map<FQN, Object> fqnObjectMap =
            FQNUtils.generateFQNMap(PmsYamlUtils.readTree(entity.getPipelineYaml()).getNode().getCurrJsonNode());
        fqnObjectMap.keySet().forEach(
            fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
      } catch (IOException e) {
        throw new InvalidRequestException("Invalid merged pipeline yaml");
      }
      // update status to in progress
      preflightService.updateStatus(entity.getUuid(), PreFlightStatus.IN_PROGRESS, null);

      List<EntityDetail> connectorUsages = entityDetails.stream()
                                               .filter(entityDetail -> entityDetail.getType() == EntityType.CONNECTORS)
                                               .collect(Collectors.toList());
      List<ConnectorCheckResponse> connectorCheckResponses =
          preflightService.updateConnectorCheckResponses(entity.getAccountIdentifier(), entity.getOrgIdentifier(),
              entity.getProjectIdentifier(), entity.getUuid(), fqnToObjectMapMergedYaml, connectorUsages);
      preflightService.updateStatus(
          entity.getUuid(), PreflightCommonUtils.getOverallStatus(connectorCheckResponses), null);
    } catch (Exception e) {
      log.error("Error occurred while handling preflight check", e);
      preflightService.updateStatus(
          entity.getUuid(), PreFlightStatus.FAILURE, PreflightCommonUtils.getInternalIssueErrorInfo());
    }
  }
}
