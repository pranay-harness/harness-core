package io.harness.gitsync.core.callback;

import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;

public class GitCommandCallbackLogContext extends AutoLogContext {
  public GitCommandCallbackLogContext(ImmutableMap<String, String> context, OverrideBehavior behavior) {
    super(context, behavior);
  }
}
