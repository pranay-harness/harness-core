package io.harness.ccm.graphql.dto.common;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPoint {
  @GraphQLNonNull Reference key;
  @GraphQLNonNull Number value;
}
