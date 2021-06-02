package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnsupportedOperationException;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.YamlChangeSetHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(DX)
public class YamlChangeSetEventHandlerFactory {
  private BranchPushEventYamlChangeSetHandler branchPushEventYamlChangeSetHandler;

  public YamlChangeSetHandler getChangeSetHandler(YamlChangeSetDTO yamlChangeSetDTO) {
    switch (yamlChangeSetDTO.getEventType()) {
      case BRANCH_PUSH:
        return branchPushEventYamlChangeSetHandler;
      default:
        throw new UnsupportedOperationException(
            "No yaml change set handler registered for event type : " + yamlChangeSetDTO.getEventType());
    }
  }
}
