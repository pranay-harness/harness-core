package software.wings.graphql.schema.mutation;

import lombok.Value;
import software.wings.graphql.schema.type.QLApplicationInput;

@Value
public class QLCreateApplicationParameters {
  private QLApplicationInput application;
}
