package io.harness.gitsync.common.helper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.dtos.GitPRFileDTO;
import io.harness.gitsync.common.dtos.GitPrFileListDTO;
import io.harness.product.ci.scm.proto.PRFile;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class PRFileListMapper {
  public GitPrFileListDTO toGitPRFileListDTO(List<PRFile> prFileList) {
    List<GitPRFileDTO> gitPrFileDTOList = new ArrayList<>();
    prFileList.forEach(prFile -> {
      ChangeType changeType = ChangeType.MODIFY;
      if (prFile.getAdded()) {
        changeType = ChangeType.ADD;
      } else if (prFile.getDeleted()) {
        changeType = ChangeType.DELETE;
      } else if (prFile.getRenamed()) {
        changeType = ChangeType.RENAME;
      }
      gitPrFileDTOList.add(GitPRFileDTO.builder().changeType(changeType).path(prFile.getPath()).build());
    });
    return GitPrFileListDTO.builder().prFileList(gitPrFileDTOList).build();
  }
}
