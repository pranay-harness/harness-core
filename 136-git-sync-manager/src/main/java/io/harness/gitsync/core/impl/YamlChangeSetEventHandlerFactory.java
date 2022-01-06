/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
  private BranchSyncEventYamlChangeSetHandler branchSyncEventYamlChangeSetHandler;

  public YamlChangeSetHandler getChangeSetHandler(YamlChangeSetDTO yamlChangeSetDTO) {
    switch (yamlChangeSetDTO.getEventType()) {
      case BRANCH_PUSH:
        return branchPushEventYamlChangeSetHandler;
      case BRANCH_SYNC:
        return branchSyncEventYamlChangeSetHandler;
      default:
        throw new UnsupportedOperationException(
            "No yaml change set handler registered for event type : " + yamlChangeSetDTO.getEventType());
    }
  }
}
