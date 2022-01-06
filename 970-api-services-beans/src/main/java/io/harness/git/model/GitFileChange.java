/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.git.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder(toBuilder = true)
@FieldNameConstants(innerTypeName = "GitFileChangeKeys")
public class GitFileChange {
  private String filePath;
  private String fileContent;
  private String rootPath;
  private String rootPathId;
  private String accountId;
  private ChangeType changeType;
  private String oldFilePath;
  @JsonIgnore @SchemaIgnore private boolean syncFromGit;

  private String commitId;
  private String objectId;
  private String processingCommitId;
  private boolean changeFromAnotherCommit;
  private Long commitTimeMs;
  private Long processingCommitTimeMs;
  private String commitMessage;
  private String processingCommitMessage;
}
