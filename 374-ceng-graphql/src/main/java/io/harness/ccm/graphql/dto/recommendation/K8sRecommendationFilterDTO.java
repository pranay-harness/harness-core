package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.ResourceType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRecommendationFilterDTO {
  List<String> ids;
  List<String> names;
  List<String> namespaces;
  List<String> clusterNames;
  List<ResourceType> resourceTypes;

  Double minSaving;
  Double minCost;

  Long offset;
  Long limit;
}