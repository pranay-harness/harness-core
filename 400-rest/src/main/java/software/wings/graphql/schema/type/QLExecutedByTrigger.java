package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByTriggerKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLExecutedByTrigger implements QLCause, QLContextedObject {
  private Map<String, Object> context;
}
