package software.wings.graphql.schema.mutation.application;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLApplicationInput;

@Value
@Builder
public class QLUpdateApplicationParameters {
  private String applicationId;
  private QLApplicationInput application;
}
