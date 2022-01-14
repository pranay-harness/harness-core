package software.wings.graphql.schema.query;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDC)
public class QLEventsConfigQueryParameters {
  private String eventsConfigId;
  private String appId;
  private String name;
}
