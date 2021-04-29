package io.harness.ccm.dto.graphql.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NodeRecommendationDTO implements RecommendationDetailsDTO {
  String id;
}
