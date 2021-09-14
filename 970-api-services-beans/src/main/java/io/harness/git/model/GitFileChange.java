/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
