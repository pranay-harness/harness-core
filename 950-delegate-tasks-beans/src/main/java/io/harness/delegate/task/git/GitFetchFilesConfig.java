package io.harness.delegate.task.git;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class GitFetchFilesConfig {
  String identifier;
  String manifestType;
  GitStoreDelegateConfig gitStoreDelegateConfig;
  boolean succeedIfFileNotFound;
}
