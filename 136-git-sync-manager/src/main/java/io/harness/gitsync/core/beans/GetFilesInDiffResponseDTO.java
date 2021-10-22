package io.harness.gitsync.core.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitDiffResultFileDTO;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DX)
public class GetFilesInDiffResponseDTO {
  List<GitFileChangeDTO> gitFileChangeDTOList;
  List<GitDiffResultFileDTO> prFilesTobeProcessed;
  String processingCommitId;
  String commitMessage;
}
