package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.GitFileChange;

import java.util.List;

@OwnedBy(DX)
public interface YamlService {
  void processChangeSet(List<GitFileChange> gitFileChanges);
}
