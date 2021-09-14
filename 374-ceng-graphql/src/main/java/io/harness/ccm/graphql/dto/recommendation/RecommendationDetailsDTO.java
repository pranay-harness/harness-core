/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.recommendation;

import io.leangen.graphql.annotations.types.GraphQLUnion;

@GraphQLUnion(name = "recommendationDetails", description = "This union of all types of recommendations",
    possibleTypes = {WorkloadRecommendationDTO.class, NodeRecommendationDTO.class})
public interface RecommendationDetailsDTO {}
