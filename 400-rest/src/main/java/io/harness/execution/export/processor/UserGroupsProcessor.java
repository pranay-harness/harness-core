/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.metadata.ApprovalMetadata;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.execution.export.metadata.PipelineExecutionMetadata;
import io.harness.execution.export.metadata.PipelineStageExecutionMetadata;

import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.UserGroupService;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@OwnedBy(CDC)
@Value
public class UserGroupsProcessor implements ExportExecutionsProcessor {
  @Inject @NonFinal @Setter UserGroupService userGroupService;

  Multimap<String, ApprovalMetadata> userGroupIdToApprovalMetadata;

  public UserGroupsProcessor() {
    this.userGroupIdToApprovalMetadata = ArrayListMultimap.create();
  }

  public void visitExecutionMetadata(ExecutionMetadata executionMetadata) {
    if (!(executionMetadata instanceof PipelineExecutionMetadata)) {
      return;
    }

    PipelineExecutionMetadata pipelineExecutionMetadata = (PipelineExecutionMetadata) executionMetadata;
    if (isEmpty(pipelineExecutionMetadata.getStages())) {
      return;
    }

    for (PipelineStageExecutionMetadata stage : pipelineExecutionMetadata.getStages()) {
      if (stage == null || stage.getApprovalData() == null || isEmpty(stage.getApprovalData().getUserGroupIds())) {
        continue;
      }

      stage.getApprovalData().getUserGroupIds().forEach(
          userGroupId -> userGroupIdToApprovalMetadata.put(userGroupId, stage.getApprovalData()));
    }
  }

  public void process() {
    if (userGroupIdToApprovalMetadata.isEmpty()) {
      return;
    }

    List<UserGroup> userGroups =
        userGroupService.fetchUserGroupNamesFromIdsUsingSecondary(userGroupIdToApprovalMetadata.keySet());
    if (isEmpty(userGroups)) {
      return;
    }

    for (UserGroup userGroup : userGroups) {
      Collection<ApprovalMetadata> approvalMetadataCollection = userGroupIdToApprovalMetadata.get(userGroup.getUuid());
      if (isEmpty(approvalMetadataCollection)) {
        continue;
      }

      approvalMetadataCollection.forEach(approvalMetadata -> approvalMetadata.addUserGroup(userGroup.getName()));
    }
  }
}
