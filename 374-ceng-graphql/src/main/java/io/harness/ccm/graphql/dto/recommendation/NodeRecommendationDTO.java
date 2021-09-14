/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequest;
import io.harness.ccm.commons.beans.recommendation.models.RecommendationResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NodeRecommendationDTO implements RecommendationDetailsDTO {
  String id;
  NodePoolId nodePoolId;

  RecommendClusterRequest resourceRequirement;

  RecommendationResponse current;
  RecommendationResponse recommended;
}
