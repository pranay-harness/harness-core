package io.harness.yaml.extended.ci.codebase.impl;

import io.harness.yaml.extended.ci.codebase.CodeBaseSpec;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitHubCodeBase implements CodeBaseSpec {
  @NotNull String connectorRef;
  @NotNull String repoPath;
}
