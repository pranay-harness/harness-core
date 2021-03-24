package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLArtifactSelectionType {
  FROM_TRIGGERING_ARTIFACT,
  FROM_TRIGGERING_PIPELINE,
  FROM_PAYLOAD_SOURCE,
  LAST_COLLECTED,
  LAST_DEPLOYED_WORKFLOW,
  LAST_DEPLOYED_PIPELINE
}
