package io.harness.gitsync.core.callback;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;

@OwnedBy(DX)
public class GitCommandCallbackLogContext extends AutoLogContext {
  public GitCommandCallbackLogContext(ImmutableMap<String, String> context, OverrideBehavior behavior) {
    super(context, behavior);
  }
}
