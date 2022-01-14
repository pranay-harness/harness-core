package software.wings.graphql.schema.query;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(CDC)
public class QLEventsConfigsQueryParameters {
  private String appId;
}
