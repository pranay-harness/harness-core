package software.wings.graphql.schema.type;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByTriggerKeys")
public class QLExecutedByTrigger implements QLCause, QLContextedObject {
  private Map<String, Object> context;
}
