package io.harness.gitsync.common.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class FileBatchResponseMapper {
  public List<GitFileChangeDTO> createGitFileChangeList(FileBatchContentResponse fileBatchContentResponse) {
    if (fileBatchContentResponse == null || isEmpty(fileBatchContentResponse.getFileContentsList())) {
      return Collections.emptyList();
    }
    return fileBatchContentResponse.getFileContentsList()
        .stream()
        .map(fileResponse -> mapToGitFileChange(fileResponse))
        .collect(toList());
  }

  private static GitFileChangeDTO mapToGitFileChange(FileContent fileResponse) {
    return GitFileChangeDTO.builder()
        .changeSetId(generateUuid())
        .commitId(fileResponse.getCommitId())
        .content(fileResponse.getContent())
        .error(fileResponse.getError())
        .objectId(fileResponse.getBlobId())
        .path(fileResponse.getPath())
        .status(fileResponse.getStatus())
        .build();
  }
}
