/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.recommendation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecommendationsDTO {
  List<RecommendationItemDTO> items;

  long offset;
  long limit;
}
