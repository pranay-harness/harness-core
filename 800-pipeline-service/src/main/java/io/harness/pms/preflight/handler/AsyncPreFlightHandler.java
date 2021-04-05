package io.harness.pms.preflight.handler;

import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNUtils;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.preflight.PreFlightCause;
import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightResolution;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.connector.ConnectorCheckResponse.ConnectorCheckResponseBuilder;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.entity.PreFlightEntity.PreFlightEntityKeys;
import io.harness.pms.sdk.preflight.PreFlightCheckMetadata;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.preflight.PreFlightRepository;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.api.client.util.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PIPELINE)
@Builder
@Slf4j
public class AsyncPreFlightHandler implements Runnable {
  private final PreFlightEntity entity;
  private final PreFlightRepository preFlightRepository;
  private final ConnectorResourceClient connectorResourceClient;
  private final PipelineSetupUsageHelper pipelineSetupUsageHelper;
  private Map<String, Object> fqnToObjectMapMergedYaml;

  private static final int PAGE = 0;
  private static final int SIZE = 100;

  @Override
  public void run() {
    log.info("Handling preflight check with id " + entity.getUuid() + " for pipeline with id "
        + entity.getPipelineIdentifier());
    try {
      fqnToObjectMapMergedYaml = new HashMap<>();
      Map<FQN, Object> fqnObjectMap =
          FQNUtils.generateFQNMap(YamlUtils.readTree(entity.getPipelineYaml()).getNode().getCurrJsonNode());
      fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid merged pipeline yaml");
    }

    List<EntityDetail> allReferredUsages = pipelineSetupUsageHelper.getReferrencesOfPipeline(
        entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier(),
        entity.getPipelineIdentifier(), entity.getPipelineYaml(), EntityType.CONNECTORS);
    Map<Scope, List<String>> scopeToConnectorIdentifiers = new HashMap<>();
    Map<String, String> connectorIdentifierToFqn = new HashMap<>();
    for (EntityDetail connector : allReferredUsages) {
      IdentifierRef ref = (IdentifierRef) connector.getEntityRef();
      Scope refScope = ref.getScope();
      String refIdentifier = ref.getIdentifier();
      Map<String, String> metadata = ref.getMetadata();
      String fqn = metadata.get(PreFlightCheckMetadata.FQN);
      connectorIdentifierToFqn.put(refIdentifier, fqn);

      if (refScope == Scope.UNKNOWN) {
        throw new InvalidRequestException("UNKNOWN scope not supported");
      }
      List<String> existingIdentifiers = scopeToConnectorIdentifiers.getOrDefault(refScope, Lists.newArrayList());
      existingIdentifiers.add(refIdentifier);
      scopeToConnectorIdentifiers.put(refScope, existingIdentifiers);
    }

    List<ConnectorResponseDTO> connectorResponses = getConnectorResponses(scopeToConnectorIdentifiers);
    List<ConnectorCheckResponse> connectorCheckResponses =
        getConnectorCheckResponse(connectorResponses, connectorIdentifierToFqn);
    updateEntityWithConnectorCheckResponses(connectorCheckResponses);
  }

  // Todo: Move connector logic into separate class
  private List<ConnectorResponseDTO> getConnectorResponses(Map<Scope, List<String>> scopeToConnectorIdentifiers) {
    List<ConnectorResponseDTO> connectorResponses = new ArrayList<>();
    if (scopeToConnectorIdentifiers.containsKey(Scope.ACCOUNT)) {
      PageResponse<ConnectorResponseDTO> response =
          execute(connectorResourceClient.listConnectors(entity.getAccountIdentifier(), null, null, PAGE, SIZE,
              ConnectorFilterPropertiesDTO.builder()
                  .connectorIdentifiers(scopeToConnectorIdentifiers.get(Scope.ACCOUNT))
                  .build()));
      connectorResponses.addAll(response.getContent());
    }
    if (scopeToConnectorIdentifiers.containsKey(Scope.ORG)) {
      PageResponse<ConnectorResponseDTO> response = execute(connectorResourceClient.listConnectors(
          entity.getAccountIdentifier(), entity.getOrgIdentifier(), null, PAGE, SIZE,
          ConnectorFilterPropertiesDTO.builder()
              .connectorIdentifiers(scopeToConnectorIdentifiers.get(Scope.ORG))
              .build()));
      connectorResponses.addAll(response.getContent());
    }
    if (scopeToConnectorIdentifiers.containsKey(Scope.PROJECT)) {
      PageResponse<ConnectorResponseDTO> response = execute(connectorResourceClient.listConnectors(
          entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier(), PAGE, SIZE,
          ConnectorFilterPropertiesDTO.builder()
              .connectorIdentifiers(scopeToConnectorIdentifiers.get(Scope.PROJECT))
              .build()));
      connectorResponses.addAll(response.getContent());
    }
    return connectorResponses;
  }

  public List<ConnectorCheckResponse> getConnectorCheckResponse(
      List<ConnectorResponseDTO> connectorResponses, Map<String, String> connectorIdentifierToFqn) {
    List<ConnectorCheckResponse> connectorCheckResponses = new ArrayList<>();
    connectorResponses.forEach(connectorResponse -> {
      String connectorIdentifier = connectorResponse.getConnector().getIdentifier();
      String stageIdentifier = YamlUtils.getStageIdentifierFromFqn(connectorIdentifierToFqn.get(connectorIdentifier));
      ConnectorCheckResponseBuilder checkResponse = ConnectorCheckResponse.builder()
                                                        .connectorIdentifier(connectorIdentifier)
                                                        .fqn(connectorIdentifierToFqn.get(connectorIdentifier))
                                                        .stageIdentifier(stageIdentifier)
                                                        .stageName(getStageName(stageIdentifier));

      ConnectorConnectivityDetails connectorConnectivityDetails = connectorResponse.getStatus();
      checkResponse.status(getPreFlightStatus(connectorConnectivityDetails.getStatus()));

      if (EmptyPredicate.isNotEmpty(connectorConnectivityDetails.getErrorSummary())) {
        List<PreFlightCause> causes = new ArrayList<>();
        List<PreFlightResolution> resolution = new ArrayList<>();
        connectorConnectivityDetails.getErrors().forEach(error -> {
          causes.add(PreFlightCause.builder().cause(error.getReason()).build());
          resolution.add(PreFlightResolution.builder().resolution(error.getMessage()).build());
        });
        checkResponse.errorInfo(PreFlightEntityErrorInfo.builder()
                                    .causes(causes)
                                    .resolution(resolution)
                                    .summary(connectorConnectivityDetails.getErrorSummary())
                                    .build());
      }
      connectorCheckResponses.add(checkResponse.build());
    });
    List<String> availableConnectors =
        connectorResponses.stream().map(c -> c.getConnector().getIdentifier()).collect(Collectors.toList());
    for (String connectorRef : connectorIdentifierToFqn.keySet()) {
      if (availableConnectors.contains(connectorRef)) {
        continue;
      }
      String stageIdentifier = YamlUtils.getStageIdentifierFromFqn(connectorIdentifierToFqn.get(connectorRef));
      connectorCheckResponses.add(ConnectorCheckResponse.builder()
                                      .connectorIdentifier(connectorRef)
                                      .fqn(connectorIdentifierToFqn.get(connectorRef))
                                      .stageIdentifier(stageIdentifier)
                                      .stageName(getStageName(stageIdentifier))
                                      .status(PreFlightStatus.FAILURE)
                                      .errorInfo(getNotFoundErrorInfo())
                                      .build());
    }
    return connectorCheckResponses;
  }

  private PreFlightEntityErrorInfo getNotFoundErrorInfo() {
    return PreFlightEntityErrorInfo.builder()
        .summary("Connector not found or does not exist")
        .causes(
            Collections.singletonList(PreFlightCause.builder().cause("Connector not found or does not exist").build()))
        .resolution(
            Collections.singletonList(PreFlightResolution.builder().resolution("Create this connector").build()))
        .build();
  }

  private String getStageName(String identifier) {
    return ((TextNode) fqnToObjectMapMergedYaml.get("pipeline.stages." + identifier + ".name")).asText();
  }

  private PreFlightStatus getPreFlightStatus(ConnectivityStatus status) {
    switch (status) {
      case SUCCESS:
        return PreFlightStatus.SUCCESS;
      case FAILURE:
        return PreFlightStatus.FAILURE;
      case PARTIAL:
      default:
        return PreFlightStatus.IN_PROGRESS;
    }
  }

  private void updateEntityWithConnectorCheckResponses(List<ConnectorCheckResponse> connectorCheckResponses) {
    Criteria criteria = Criteria.where(PreFlightEntityKeys.uuid).is(entity.getUuid());
    entity.setConnectorCheckResponse(connectorCheckResponses);
    preFlightRepository.update(criteria, entity);
  }
}
