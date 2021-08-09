package software.wings.graphql.schema.query;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public class QLApprovalDetailsInput {
  String applicationId;
  String executionId;
}
