package io.harness.delegate.task.git;

import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitFetchFilesConfig {
  String identifier;
  GitStoreDelegateConfig gitStoreDelegateConfig;
  boolean succeedIfFileNotFound;
}
