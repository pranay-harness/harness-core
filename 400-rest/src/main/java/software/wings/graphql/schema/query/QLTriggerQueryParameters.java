package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTriggerQueryParametersKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLTriggerQueryParameters {
  private String triggerId;
  private String triggerName;
  private String applicationId;
}
