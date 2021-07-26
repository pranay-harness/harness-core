package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewSortCriteria {
  @GraphQLNonNull QLCEViewSortType sortType;
  @GraphQLNonNull QLCESortOrder sortOrder;
}
