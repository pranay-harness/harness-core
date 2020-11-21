package io.harness.gitsync.core.service;

import io.harness.git.model.GitFileChange;

import java.util.List;

public interface YamlService {
  void processChangeSet(List<GitFileChange> gitFileChanges);
}
