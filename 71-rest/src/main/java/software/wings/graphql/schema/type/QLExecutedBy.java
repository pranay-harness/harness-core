package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByKeys")
public class QLExecutedBy implements QLCause {
  public enum QLExecuteOptions { WEB_UI }

  private QLUser user;
  private QLExecuteOptions using;
}
