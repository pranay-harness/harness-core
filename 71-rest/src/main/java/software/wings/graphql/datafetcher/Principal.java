package software.wings.graphql.datafetcher;

import lombok.Builder;
import lombok.Value;
import software.wings.resources.graphql.TriggeredByType;

@Value
@Builder
public class Principal {
  private TriggeredByType triggeredByType;
  private String triggeredById;
}
