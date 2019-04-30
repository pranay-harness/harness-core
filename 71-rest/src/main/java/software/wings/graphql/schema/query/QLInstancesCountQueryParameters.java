package software.wings.graphql.schema.query;

import lombok.Value;
import software.wings.graphql.schema.type.QLInstanceCountType;

@Value
public class QLInstancesCountQueryParameters {
  private String accountId;
  private QLInstanceCountType instanceCountType;
}
