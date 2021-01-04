package software.wings.graphql.schema.type.usergroup;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLUserGroupFilter {
  QLIdFilter user;
}
