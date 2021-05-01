package io.harness.ccm.dto.graphql.recommendation;

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
