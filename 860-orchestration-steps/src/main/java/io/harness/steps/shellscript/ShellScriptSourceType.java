package io.harness.steps.shellscript;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface ShellScriptSourceType {
  String GIT = "Git";
  String INLINE = "Inline";
}
