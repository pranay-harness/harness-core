package io.harness.ccm.graphql.dto.recommendation;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;

import io.leangen.graphql.annotations.GraphQLQuery;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkloadRecommendationDTO implements RecommendationDetailsDTO {
  String id;
  @GraphQLQuery(description = "use items.containerRecommendation", deprecationReason = "")
  @Deprecated
  Map<String, ContainerRecommendation> containerRecommendations;
  List<ContainerHistogramDTO> items;
  Cost lastDayCost;
}
