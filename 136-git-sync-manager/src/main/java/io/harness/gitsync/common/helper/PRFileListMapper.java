/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.dtos.GitDiffResultFileDTO;
import io.harness.gitsync.common.dtos.GitDiffResultFileDTO.GitDiffResultFileDTOBuilder;
import io.harness.gitsync.common.dtos.GitDiffResultFileListDTO;
import io.harness.product.ci.scm.proto.PRFile;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class PRFileListMapper {
  public GitDiffResultFileListDTO toGitDiffResultFileListDTO(List<PRFile> prFileList) {
    List<GitDiffResultFileDTO> gitDiffResultFileDTOList = new ArrayList<>();
    GitDiffResultFileDTOBuilder gitDiffResultFileDTOBuilder = GitDiffResultFileDTO.builder();
    if (prFileList != null) {
      prFileList.forEach(prFile -> {
        ChangeType changeType = ChangeType.MODIFY;
        if (prFile.getAdded()) {
          changeType = ChangeType.ADD;
        } else if (prFile.getDeleted()) {
          changeType = ChangeType.DELETE;
        } else if (prFile.getRenamed()) {
          changeType = ChangeType.RENAME;
          gitDiffResultFileDTOBuilder.prevFilePath(prFile.getPrevFilePath());
        }
        gitDiffResultFileDTOList.add(gitDiffResultFileDTOBuilder.changeType(changeType).path(prFile.getPath()).build());
      });
    }
    return GitDiffResultFileListDTO.builder().prFileList(gitDiffResultFileDTOList).build();
  }
}
