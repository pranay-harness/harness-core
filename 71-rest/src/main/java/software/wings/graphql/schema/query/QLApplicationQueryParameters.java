package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLApplicationQueryParameters {
  private String applicationId;
  private String name;
}
