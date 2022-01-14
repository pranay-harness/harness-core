package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class GitFileChangeDTO {
  /***
   * The changeSetId is a unique id which we will generate for this file
   */
  String changeSetId;
  String content;
  int status;
  String path;
  String commitId;
  String objectId;
  String error;
}
