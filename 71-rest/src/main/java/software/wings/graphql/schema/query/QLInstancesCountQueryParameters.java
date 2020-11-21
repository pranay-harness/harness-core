package software.wings.graphql.schema.query;

import software.wings.graphql.schema.type.QLInstanceCountType;

import lombok.Value;

@Value
public class QLInstancesCountQueryParameters {
  private String accountId;
  private QLInstanceCountType instanceCountType;
}
