package io.harness.ccm.graphql.dto.recommendation;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;

import io.leangen.graphql.annotations.GraphQLQuery;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkloadRecommendationDTO implements RecommendationDetailsDTO {
  @GraphQLQuery(description = "use items.containerRecommendation", deprecationReason = "")
  Map<String, ContainerRecommendation> containerRecommendations;
  List<ContainerHistogramDTO> items;
}
