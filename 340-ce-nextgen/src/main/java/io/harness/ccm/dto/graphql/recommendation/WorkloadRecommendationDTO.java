package io.harness.ccm.dto.graphql.recommendation;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkloadRecommendationDTO implements RecommendationDetailsDTO {
  Map<String, ContainerRecommendation> containerRecommendations;
  List<ContainerHistogramDTO> items;
}
