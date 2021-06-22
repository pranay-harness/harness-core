package io.harness.ccm.graphql.dto.common;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Reference {
  @GraphQLNonNull String id;
  @GraphQLNonNull String name;
  @GraphQLNonNull String type;
}
