package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByTriggerKeys")
public class QLExecutedByTrigger implements QLCause, QLContextedObject {
  private Map<String, Object> context;
}
