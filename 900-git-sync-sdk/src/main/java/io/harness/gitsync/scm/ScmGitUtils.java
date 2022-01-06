/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.PushFileResponse;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmCreateFileResponse;
import io.harness.gitsync.scm.beans.ScmDeleteFileResponse;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.gitsync.scm.beans.ScmUpdateFileResponse;
import io.harness.ng.core.EntityDetail;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class ScmGitUtils {
  public ScmPushResponse createScmPushResponse(String yaml, GitEntityInfo gitEntityInfo,
      PushFileResponse pushFileResponse, EntityDetail entityDetail, ChangeType changeType) {
    switch (changeType) {
      case ADD:
        return createScmCreateFileResponse(yaml, gitEntityInfo, pushFileResponse, entityDetail);
      case DELETE:
        return createScmDeleteFileResponse(gitEntityInfo, pushFileResponse, entityDetail);
      case MODIFY:
        return createScmUpdateFileResponse(yaml, gitEntityInfo, pushFileResponse, entityDetail);
      default:
        throw new UnexpectedException("Unexpected changetype encountered");
    }
  }

  public ScmCreateFileResponse createScmCreateFileResponse(
      String yaml, GitEntityInfo gitEntityInfo, PushFileResponse pushFileResponse, EntityDetail entityDetail) {
    return ScmCreateFileResponse.builder()
        .folderPath(gitEntityInfo.getFolderPath())
        .filePath(gitEntityInfo.getFilePath())
        .pushToDefaultBranch(pushFileResponse.getIsDefault())
        .yamlGitConfigId(gitEntityInfo.getYamlGitConfigId())
        .accountIdentifier(entityDetail.getEntityRef().getAccountIdentifier())
        .orgIdentifier(entityDetail.getEntityRef().getOrgIdentifier())
        .projectIdentifier(entityDetail.getEntityRef().getProjectIdentifier())
        .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
        .branch(gitEntityInfo.getBranch())
        .commitId(pushFileResponse.getCommitId())
        .build();
  }

  public ScmUpdateFileResponse createScmUpdateFileResponse(
      String yaml, GitEntityInfo gitEntityInfo, PushFileResponse pushFileResponse, EntityDetail entityDetail) {
    return ScmUpdateFileResponse.builder()
        .folderPath(gitEntityInfo.getFolderPath())
        .filePath(gitEntityInfo.getFilePath())
        .pushToDefaultBranch(pushFileResponse.getIsDefault())
        .yamlGitConfigId(gitEntityInfo.getYamlGitConfigId())
        .accountIdentifier(entityDetail.getEntityRef().getAccountIdentifier())
        .orgIdentifier(entityDetail.getEntityRef().getOrgIdentifier())
        .projectIdentifier(entityDetail.getEntityRef().getProjectIdentifier())
        .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
        .branch(gitEntityInfo.getBranch())
        .commitId(pushFileResponse.getCommitId())
        .build();
  }

  public ScmDeleteFileResponse createScmDeleteFileResponse(
      GitEntityInfo gitEntityInfo, PushFileResponse pushFileResponse, EntityDetail entityDetail) {
    return ScmDeleteFileResponse.builder()
        .folderPath(gitEntityInfo.getFolderPath())
        .filePath(gitEntityInfo.getFilePath())
        .pushToDefaultBranch(pushFileResponse.getIsDefault())
        .yamlGitConfigId(gitEntityInfo.getYamlGitConfigId())
        .accountIdentifier(entityDetail.getEntityRef().getAccountIdentifier())
        .orgIdentifier(entityDetail.getEntityRef().getOrgIdentifier())
        .projectIdentifier(entityDetail.getEntityRef().getProjectIdentifier())
        .objectId(null)
        .branch(gitEntityInfo.getBranch())
        .commitId(pushFileResponse.getCommitId())
        .build();
  }

  public String createFilePath(String folderPath, String filePath) {
    if (isEmpty(folderPath)) {
      throw new InvalidRequestException("Folder path cannot be empty");
    }
    if (isEmpty(filePath)) {
      throw new InvalidRequestException("File path cannot be empty");
    }
    String updatedFolderPath = folderPath.endsWith("/") ? folderPath : folderPath.concat("/");
    String folderPathWithoutStartingSlash =
        updatedFolderPath.charAt(0) != '/' ? updatedFolderPath : updatedFolderPath.substring(1);
    String updatedFilePath = filePath.charAt(0) != '/' ? filePath : filePath.substring(1);
    return folderPathWithoutStartingSlash + updatedFilePath;
  }
}
