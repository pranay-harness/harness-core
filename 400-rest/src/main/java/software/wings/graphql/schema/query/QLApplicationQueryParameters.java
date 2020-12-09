package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLApplicationQueryParameters {
  private String applicationId;
  private String name;
}
