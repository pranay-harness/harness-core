package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "QLPageInfoKeys")
public class QLPageInfo implements QLObject {
  Integer limit;
  Integer offset;

  Boolean hasMore;
  Integer total;
}
