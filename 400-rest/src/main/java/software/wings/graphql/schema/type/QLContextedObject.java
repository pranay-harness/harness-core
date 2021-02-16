package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;

@TargetModule(Module._380_CG_GRAPHQL)
public interface QLContextedObject {
  Map<String, Object> getContext();
}
