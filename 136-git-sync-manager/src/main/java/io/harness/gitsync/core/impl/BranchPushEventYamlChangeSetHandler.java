package io.harness.gitsync.core.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.YamlChangeSetHandler;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.DX)
@Singleton
public class BranchPushEventYamlChangeSetHandler implements YamlChangeSetHandler {
  @Override
  public void process(YamlChangeSetDTO yamlChangeSetDTO) {}
}
