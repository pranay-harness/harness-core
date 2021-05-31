package io.harness.ccm.graphql.query.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.graphql.dto.recommendation.NodeRecommendationDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;

import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class NodeRecommendationQuery {
  @GraphQLQuery(description = "nodeRecommendation")
  public NodeRecommendationDTO nodeRecommendation(@GraphQLContext RecommendationItemDTO recommendationItem,
      @GraphQLArgument(name = "startTime", description = "defaults to Now().minusDays(7)") OffsetDateTime startTime,
      @GraphQLArgument(name = "endTime", description = "defaults to Now()") OffsetDateTime endTime,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    String ceRecommendationId = recommendationItem.getId();
    // TODO(UTSAV): nodeRecommendationService.getById(accountIdentifier, ceRecommendationId, start, end);

    return NodeRecommendationDTO.builder().id("id0").build();
  }
}
