package software.wings.graphql.schema.type.usergroup;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLUserGroupFilter {
  QLIdFilter user;
}
