package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeploymentOutcomeKeys")
public class QLDeploymentOutcome implements QLOutcome {
  private QLExecution execution;
  private QLService service;
}
