package io.harness.ccm.graphql.dto.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NodeRecommendationDTO implements RecommendationDetailsDTO {
  String id;
}
