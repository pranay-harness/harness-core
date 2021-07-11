package io.harness.gitsync.core.beans;

import io.harness.gitsync.common.dtos.GitDiffResultFileDTO;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetFilesInDiffResponseDTO {
  List<GitFileChangeDTO> gitFileChangeDTOList;
  List<GitDiffResultFileDTO> prFilesTobeProcessed;
}
