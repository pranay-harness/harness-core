package software.wings.graphql.schema.type.aggregation.anomaly;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.CE_ANOMALIES)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateAnomalyPayLoad implements QLMutationPayload {
  private String clientMutationId;
  private QLAnomalyData anomaly;
}
