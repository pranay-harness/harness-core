package io.harness.yaml.extended.ci.codebase.impl;

import io.harness.yaml.extended.ci.codebase.CodeBaseSpec;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class GitHubCodeBase implements CodeBaseSpec {
  @NotNull String connectorRef;
  @NotNull String repoPath;
}
