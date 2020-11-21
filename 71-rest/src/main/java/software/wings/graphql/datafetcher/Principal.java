package software.wings.graphql.datafetcher;

import software.wings.resources.graphql.TriggeredByType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Principal {
  private TriggeredByType triggeredByType;
  private String triggeredById;
}
