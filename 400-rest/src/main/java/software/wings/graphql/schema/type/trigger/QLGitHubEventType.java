package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public enum QLGitHubEventType {
  PULL_REQUEST,
  PUSH,
  PING,
  DELETE,
  ANY,
  RELEASE,
  PACKAGE
}
