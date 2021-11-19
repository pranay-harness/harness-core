package io.harness.gitsync.common.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@EqualsAndHashCode()
@FieldNameConstants(innerTypeName = "FileProcessingResponseKeys")
public class FileProcessingResponseDTO {
  FileProcessingStatus fileProcessingStatus;
  String errorMessage;
  String filePath;
}
