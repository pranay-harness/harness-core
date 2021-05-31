package io.harness.ccm.graphql.dto.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NodeRecommendationDTO implements RecommendationDetailsDTO {
  String id;
  // TODO(UTSAV): Integer or int
  Integer sumCpu;
  Integer sumMemory;
  Integer maxCpu;
  Integer maxMemory;
  String currentCloudProvider; // gcp/google, azure, aws
  String currentService; // gke, aks, eks
}
