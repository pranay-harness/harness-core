package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

/**
 * Interface for any handler to handle changeset from queue.
 * Same need to be registered in {@link io.harness.gitsync.core.impl.YamlChangeSetEventHandlerFactory}
 */
@OwnedBy(DX)
public interface YamlChangeSetHandler {
  YamlChangeSetStatus process(YamlChangeSetDTO yamlChangeSetDTO);
}
