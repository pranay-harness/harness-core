package io.harness.gitsync.common.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ProcessingResponseKeys")
public class GitToHarnessProcessingResponseDTO {
  List<FileProcessingResponseDTO> fileResponses;
  String accountId;
  MsvcProcessingFailureStage msvcProcessingFailureStage;
}
