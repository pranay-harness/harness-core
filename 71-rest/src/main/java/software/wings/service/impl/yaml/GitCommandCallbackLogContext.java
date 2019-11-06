package software.wings.service.impl.yaml;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class GitCommandCallbackLogContext extends AutoLogContext {
  public GitCommandCallbackLogContext(ImmutableMap<String, String> context, OverrideBehavior behavior) {
    super(context, behavior);
  }
}
