package io.harness.pms.preflight.connector;

import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.preflight.PreFlightCause;
import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightResolution;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.PreflightCommonUtils;
import io.harness.pms.preflight.connector.ConnectorCheckResponse.ConnectorCheckResponseBuilder;
import io.harness.pms.sdk.preflight.PreFlightCheckMetadata;
import io.harness.pms.yaml.PmsYamlUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ConnectorPreflightHandler {
  private static final int PAGE = 0;
  private static final int SIZE = 100;

  @Inject private ConnectorResourceClient connectorResourceClient;

  public List<ConnectorCheckResponse> getConnectorCheckResponseTemplate(List<EntityDetail> entityDetails) {
    return entityDetails.stream()
        .map(entityDetail
            -> ConnectorCheckResponse.builder()
                   .connectorIdentifier(entityDetail.getEntityRef().getIdentifier())
                   .status(PreFlightStatus.UNKNOWN)
                   .build())
        .collect(Collectors.toList());
  }

  public List<ConnectorCheckResponse> getConnectorCheckResponsesForReferredConnectors(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, Map<String, Object> fqnToObjectMapMergedYaml,
      List<EntityDetail> connectorUsages) {
    Map<Scope, List<String>> scopeToConnectorIdentifiers = new HashMap<>();
    Map<String, String> connectorIdentifierToFqn = new HashMap<>();
    for (EntityDetail connector : connectorUsages) {
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
    List<ConnectorResponseDTO> connectorResponses =
        getConnectorResponses(accountIdentifier, orgIdentifier, projectIdentifier, scopeToConnectorIdentifiers);
    return getConnectorCheckResponse(fqnToObjectMapMergedYaml, connectorResponses, connectorIdentifierToFqn);
  }

  public List<ConnectorResponseDTO> getConnectorResponses(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<Scope, List<String>> scopeToConnectorIdentifiers) {
    List<ConnectorResponseDTO> connectorResponses = new ArrayList<>();
    if (scopeToConnectorIdentifiers.containsKey(Scope.ACCOUNT)) {
      PageResponse<ConnectorResponseDTO> response =
          execute(connectorResourceClient.listConnectors(accountIdentifier, null, null, PAGE, SIZE,
              ConnectorFilterPropertiesDTO.builder()
                  .connectorIdentifiers(scopeToConnectorIdentifiers.get(Scope.ACCOUNT))
                  .build()));
      connectorResponses.addAll(response.getContent());
    }
    if (scopeToConnectorIdentifiers.containsKey(Scope.ORG)) {
      PageResponse<ConnectorResponseDTO> response =
          execute(connectorResourceClient.listConnectors(accountIdentifier, orgIdentifier, null, PAGE, SIZE,
              ConnectorFilterPropertiesDTO.builder()
                  .connectorIdentifiers(scopeToConnectorIdentifiers.get(Scope.ORG))
                  .build()));
      connectorResponses.addAll(response.getContent());
    }
    if (scopeToConnectorIdentifiers.containsKey(Scope.PROJECT)) {
      PageResponse<ConnectorResponseDTO> response = execute(
          connectorResourceClient.listConnectors(accountIdentifier, orgIdentifier, projectIdentifier, PAGE, SIZE,
              ConnectorFilterPropertiesDTO.builder()
                  .connectorIdentifiers(scopeToConnectorIdentifiers.get(Scope.PROJECT))
                  .build()));
      connectorResponses.addAll(response.getContent());
    }
    return connectorResponses;
  }

  public List<ConnectorCheckResponse> getConnectorCheckResponse(Map<String, Object> fqnToObjectMapMergedYaml,
      List<ConnectorResponseDTO> connectorResponses, Map<String, String> connectorIdentifierToFqn) {
    List<ConnectorCheckResponse> connectorCheckResponses = new ArrayList<>();
    connectorResponses.forEach(connectorResponse -> {
      String connectorIdentifier = connectorResponse.getConnector().getIdentifier();
      String stageIdentifier =
          PmsYamlUtils.getStageIdentifierFromFqn(connectorIdentifierToFqn.get(connectorIdentifier));
      ConnectorCheckResponseBuilder checkResponse =
          ConnectorCheckResponse.builder()
              .connectorIdentifier(connectorIdentifier)
              .fqn(connectorIdentifierToFqn.get(connectorIdentifier))
              .stageIdentifier(stageIdentifier)
              .stageName(PreflightCommonUtils.getStageName(fqnToObjectMapMergedYaml, stageIdentifier));

      ConnectorConnectivityDetails connectorConnectivityDetails = connectorResponse.getStatus();
      checkResponse.status(PreflightCommonUtils.getPreFlightStatus(connectorConnectivityDetails.getStatus()));

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
      String stageIdentifier = PmsYamlUtils.getStageIdentifierFromFqn(connectorIdentifierToFqn.get(connectorRef));
      connectorCheckResponses.add(
          ConnectorCheckResponse.builder()
              .connectorIdentifier(connectorRef)
              .fqn(connectorIdentifierToFqn.get(connectorRef))
              .stageIdentifier(stageIdentifier)
              .stageName(PreflightCommonUtils.getStageName(fqnToObjectMapMergedYaml, stageIdentifier))
              .status(PreFlightStatus.FAILURE)
              .errorInfo(PreflightCommonUtils.getNotFoundErrorInfo())
              .build());
    }
    return connectorCheckResponses;
  }
}
