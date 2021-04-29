package io.harness.ccm.dto.graphql.recommendation;

import io.leangen.graphql.annotations.types.GraphQLUnion;

@GraphQLUnion(name = "recommendationDetails", description = "This union of all types of recommendations",
    possibleTypes = {WorkloadRecommendationDTO.class, NodeRecommendationDTO.class})
public interface RecommendationDetailsDTO {}
