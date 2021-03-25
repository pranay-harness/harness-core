package io.harness.pms.preflight.handler;

import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.common.NGExpressionUtils;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.encryption.Scope;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNUtils;
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
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Builder
@Slf4j
public class AsyncPreFlightHandler implements Runnable {
  private final PreFlightEntity entity;
  private final PreFlightRepository preFlightRepository;
  private final EntitySetupUsageClient entitySetupUsageClient;
  private final ConnectorResourceClient connectorResourceClient;
  private final Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();

  private static final int PAGE = 0;
  private static final int SIZE = 100;

  @Override
  public void run() {
    log.info("Handling preflight check with id " + entity.getUuid() + " for pipeline with id "
        + entity.getPipelineIdentifier());
    try {
      Map<FQN, Object> fqnObjectMap =
          FQNUtils.generateFQNMap(YamlUtils.readTree(entity.getPipelineYaml()).getNode().getCurrJsonNode());
      fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid merged pipeline yaml");
    }

    List<EntitySetupUsageDTO> allReferredUsages =
        execute(entitySetupUsageClient.listAllReferredUsages(PAGE, SIZE, entity.getAccountIdentifier(),
            FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(entity.getAccountIdentifier(),
                entity.getOrgIdentifier(), entity.getProjectIdentifier(), entity.getPipelineIdentifier()),
            EntityType.CONNECTORS, null));

    List<String> accountLevelConnectors = new ArrayList<>();
    List<String> orgLevelConnectors = new ArrayList<>();
    List<String> projectLevelConnectors = new ArrayList<>();
    Map<String, String> connectorIdentifierToFqn = new HashMap<>();
    for (EntitySetupUsageDTO connector : allReferredUsages) {
      IdentifierRef ref = (IdentifierRef) connector.getReferredEntity().getEntityRef();
      Scope refScope = ref.getScope();
      String refIdentifier = ref.getIdentifier();
      Map<String, String> metadata = ref.getMetadata();
      String fqn = metadata.get(PreFlightCheckMetadata.FQN);

      if (!metadata.containsKey(PreFlightCheckMetadata.EXPRESSION)) {
        connectorIdentifierToFqn.put(refIdentifier, fqn);
      } else if (fqnToObjectMapMergedYaml.containsKey(fqn)) {
        String finalValue = ((TextNode) fqnToObjectMapMergedYaml.get(fqn)).asText();
        if (NGExpressionUtils.isRuntimeOrExpressionField(finalValue)) {
          continue;
        }
        IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
            finalValue, entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier());
        refScope = identifierRef.getScope();
        refIdentifier = identifierRef.getIdentifier();
        connectorIdentifierToFqn.put(refIdentifier, fqn);
      }

      switch (refScope) {
        case ACCOUNT:
          if (!accountLevelConnectors.contains(refIdentifier)) {
            accountLevelConnectors.add(refIdentifier);
          }
          break;
        case ORG:
          if (!orgLevelConnectors.contains(refIdentifier)) {
            orgLevelConnectors.add(refIdentifier);
          }
          break;
        case PROJECT:
          if (!projectLevelConnectors.contains(refIdentifier)) {
            projectLevelConnectors.add(refIdentifier);
          }
          break;
        default:
          throw new InvalidRequestException("UNKNOWN scope not supported");
      }
    }

    List<ConnectorResponseDTO> connectorResponses =
        getConnectorResponses(accountLevelConnectors, orgLevelConnectors, projectLevelConnectors);
    List<ConnectorCheckResponse> connectorCheckResponses =
        getConnectorCheckResponse(connectorResponses, connectorIdentifierToFqn);
    updateEntityWithConnectorCheckResponses(connectorCheckResponses);
  }

  private List<ConnectorResponseDTO> getConnectorResponses(
      List<String> accountLevelConnectors, List<String> orgLevelConnectors, List<String> projectLevelConnectors) {
    List<ConnectorResponseDTO> connectorResponses = new ArrayList<>();
    if (hasSome(accountLevelConnectors)) {
      PageResponse<ConnectorResponseDTO> response =
          execute(connectorResourceClient.listConnectors(entity.getAccountIdentifier(), null, null, PAGE, SIZE,
              ConnectorFilterPropertiesDTO.builder().connectorIdentifiers(accountLevelConnectors).build()));
      connectorResponses.addAll(response.getContent());
    }
    if (hasSome(orgLevelConnectors)) {
      PageResponse<ConnectorResponseDTO> response =
          execute(connectorResourceClient.listConnectors(entity.getAccountIdentifier(), entity.getOrgIdentifier(), null,
              PAGE, SIZE, ConnectorFilterPropertiesDTO.builder().connectorIdentifiers(orgLevelConnectors).build()));
      connectorResponses.addAll(response.getContent());
    }
    if (hasSome(projectLevelConnectors)) {
      PageResponse<ConnectorResponseDTO> response = execute(connectorResourceClient.listConnectors(
          entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier(), PAGE, SIZE,
          ConnectorFilterPropertiesDTO.builder().connectorIdentifiers(projectLevelConnectors).build()));
      connectorResponses.addAll(response.getContent());
    }
    return connectorResponses;
  }

  private List<ConnectorCheckResponse> getConnectorCheckResponse(
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

      if (hasSome(connectorConnectivityDetails.getErrorSummary())) {
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
    return connectorCheckResponses;
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
