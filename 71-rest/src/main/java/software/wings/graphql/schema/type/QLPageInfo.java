package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLPageInfo {
  Boolean hasMore;
  Integer total;
  Integer limit;
  Integer offset;
}
