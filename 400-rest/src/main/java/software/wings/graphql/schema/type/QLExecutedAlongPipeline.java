package software.wings.graphql.schema.type;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedAlongPipelineKeys")
public class QLExecutedAlongPipeline implements QLCause, QLContextedObject {
  private Map<String, Object> context;
}
