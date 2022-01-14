package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLInstanceCountType;

import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLInstancesCountQueryParameters {
  private String accountId;
  private QLInstanceCountType instanceCountType;
}
