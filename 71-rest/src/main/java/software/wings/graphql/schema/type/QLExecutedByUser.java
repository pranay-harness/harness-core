package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByUserKeys")
public class QLExecutedByUser implements QLCause {
  public enum QLExecuteOptions { WEB_UI }

  private QLUser user;
  private QLExecuteOptions using;
}
