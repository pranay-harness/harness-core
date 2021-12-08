package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequestDTO;
import io.harness.ccm.commons.beans.recommendation.models.RecommendationResponse;

import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@GraphQLType(name = "NodeRecommendation")
public class NodeRecommendationDTO implements RecommendationDetailsDTO {
  String id;
  NodePoolId nodePoolId;

  RecommendClusterRequestDTO resourceRequirement;

  RecommendationResponse current;
  RecommendationResponse recommended;
}
