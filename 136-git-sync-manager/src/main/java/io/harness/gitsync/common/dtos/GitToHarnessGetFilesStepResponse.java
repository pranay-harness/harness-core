package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class GitToHarnessGetFilesStepResponse {
  List<GitFileChangeDTO> gitFileChangeDTOList;
  List<GitDiffResultFileDTO> gitDiffResultFileDTOList;
  GitToHarnessProgressDTO progressRecord;
  String processingCommitId;
  String commitMessage;
}
