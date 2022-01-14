package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedAlongPipelineKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLExecutedAlongPipeline implements QLCause, QLContextedObject {
  private Map<String, Object> context;
}
