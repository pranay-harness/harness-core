package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPageInfoKeys")
public class QLPageInfo implements QLObject {
  private Integer limit;
  private Integer offset;

  private Boolean hasMore;
  private Integer total;
}
