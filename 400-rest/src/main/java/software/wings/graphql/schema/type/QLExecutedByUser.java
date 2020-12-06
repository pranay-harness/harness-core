package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByUserKeys")
public class QLExecutedByUser implements QLCause {
  private QLUser user;
  private QLExecuteOptions using;
}
