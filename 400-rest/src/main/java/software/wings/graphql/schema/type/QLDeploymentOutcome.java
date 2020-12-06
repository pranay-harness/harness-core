package software.wings.graphql.schema.type;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeploymentOutcomeKeys")
public class QLDeploymentOutcome implements QLOutcome, QLContextedObject {
  private Map<String, Object> context;
}
