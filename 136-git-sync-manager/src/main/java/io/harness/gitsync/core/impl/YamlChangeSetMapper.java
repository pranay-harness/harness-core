/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class YamlChangeSetMapper {
  YamlChangeSetDTO getYamlChangeSetDto(YamlChangeSet yamlChangeSet) {
    final int retryCount = yamlChangeSet.getRetryCount() != null ? yamlChangeSet.getRetryCount() : 0;
    return YamlChangeSetDTO.builder()
        .branch(yamlChangeSet.getBranch())
        .gitWebhookRequestAttributes(yamlChangeSet.getGitWebhookRequestAttributes())
        .eventMetadata(yamlChangeSet.getEventMetadata())
        .accountId(yamlChangeSet.getAccountId())
        .eventType(yamlChangeSet.getEventType())
        .repoUrl(yamlChangeSet.getRepoUrl())
        .changesetId(yamlChangeSet.getUuid())
        .status(yamlChangeSet.getStatus())
        .retryCount(retryCount)
        .build();
  }
}
