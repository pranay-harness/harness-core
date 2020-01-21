package software.wings.graphql.schema.type.user;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeleteUserInput {
  private String requestId;
  private String id;
}
