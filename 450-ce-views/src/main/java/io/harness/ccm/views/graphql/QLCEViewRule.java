package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.types.GraphQLType;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@GraphQLType(name = "ViewRule")
public class QLCEViewRule {
  List<QLCEViewFilter> conditions;
}
