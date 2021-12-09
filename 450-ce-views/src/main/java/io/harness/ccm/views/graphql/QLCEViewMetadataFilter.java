package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PUBLIC)
@GraphQLType(name = "ViewMetadataFilter")
public class QLCEViewMetadataFilter {
  @GraphQLNonNull String viewId;
  @GraphQLNonNull boolean isPreview;
}
