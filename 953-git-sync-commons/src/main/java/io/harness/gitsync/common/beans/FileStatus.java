package io.harness.gitsync.common.beans;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "FileStatusKeys")
@ToString(exclude = "fileContent")
public class FileStatus {
  String filePath;
  String fileContent;
  String changeType;
  String entityType;
  FileProcessStatus status;
  String errorMessage;
}