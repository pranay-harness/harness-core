package io.harness.gitsync.core.service;

import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

public interface YamlChangeSetLifeCycleManagerService {
  void handleChangeSet(YamlChangeSetDTO yamlChangeSet);
}
