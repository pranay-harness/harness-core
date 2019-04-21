package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLPipelinesQueryParameters implements QLPageQueryParameters {
  private String appId;
  private int limit;
  private int offset;
}
