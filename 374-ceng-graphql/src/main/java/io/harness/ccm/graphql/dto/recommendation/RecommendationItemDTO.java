/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.ResourceType;

import io.leangen.graphql.annotations.GraphQLNonNull;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecommendationItemDTO {
  @GraphQLNonNull @NotNull String id;
  String clusterName;
  String namespace;
  String resourceName;
  Double monthlySaving;
  Double monthlyCost;
  @GraphQLNonNull @NotNull ResourceType resourceType;
  RecommendationDetailsDTO recommendationDetails;
}
