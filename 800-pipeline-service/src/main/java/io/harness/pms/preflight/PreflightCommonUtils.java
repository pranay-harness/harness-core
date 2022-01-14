package io.harness.pms.preflight;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.inputset.PipelineInputResponse;

import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PreflightCommonUtils {
  public PreFlightEntityErrorInfo getNotFoundErrorInfo() {
    return PreFlightEntityErrorInfo.builder()
        .summary("Connector not found or does not exist")
        .causes(
            Collections.singletonList(PreFlightCause.builder().cause("Connector not found or does not exist").build()))
        .resolution(
            Collections.singletonList(PreFlightResolution.builder().resolution("Create this connector").build()))
        .build();
  }

  public PreFlightEntityErrorInfo getInvalidConnectorInfo() {
    return PreFlightEntityErrorInfo.builder()
        .summary("Connector not valid")
        .causes(Collections.singletonList(
            PreFlightCause.builder().cause("The connector YAML provided on git is invalid").build()))
        .resolution(
            Collections.singletonList(PreFlightResolution.builder().resolution("Fix the connector yaml").build()))
        .build();
  }

  public PreFlightEntityErrorInfo getInternalIssueErrorInfo() {
    return PreFlightEntityErrorInfo.builder()
        .summary("Error connecting to systems upstream")
        .causes(Collections.singletonList(PreFlightCause.builder().cause("Internal Server Error").build()))
        .build();
  }
  public String getStageName(Map<String, Object> fqnToObjectMapMergedYaml, String identifier) {
    return ((TextNode) fqnToObjectMapMergedYaml.get("pipeline.stages." + identifier + ".name")).asText();
  }

  public PreFlightStatus getPreFlightStatus(ConnectivityStatus status) {
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

  public PreFlightStatus getOverallStatus(
      List<ConnectorCheckResponse> connectorCheckResponse, List<PipelineInputResponse> pipelineInputResponses) {
    if (getConnectorCheckStatus(connectorCheckResponse) == PreFlightStatus.FAILURE
        || getPipelineInputStatus(pipelineInputResponses) == PreFlightStatus.FAILURE) {
      return PreFlightStatus.FAILURE;
    }
    return PreFlightStatus.SUCCESS;
  }
}
