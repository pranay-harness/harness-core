package io.harness.gitsync.common.beans;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "FileProcessingResponseKeys")
public class FileProcessingResponseDTO {
  FileProcessingStatus fileProcessingStatus;
  String errorMessage;
  String filePath;
}
