package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public enum QLConditionType {
  ON_NEW_ARTIFACT,
  ON_PIPELINE_COMPLETION,
  ON_SCHEDULE,
  ON_WEBHOOK
}
