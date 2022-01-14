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
