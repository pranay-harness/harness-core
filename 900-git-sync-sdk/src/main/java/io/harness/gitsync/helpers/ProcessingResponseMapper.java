package io.harness.gitsync.helpers;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileProcessingResponse;
import io.harness.gitsync.ProcessingFailureStage;
import io.harness.gitsync.ProcessingResponse;
import io.harness.gitsync.common.beans.FileProcessingResponseDTO;
import io.harness.gitsync.common.beans.FileProcessingStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO.GitToHarnessProcessingResponseDTOBuilder;
import io.harness.gitsync.common.beans.MsvcProcessingFailureStage;

import com.google.protobuf.StringValue;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class ProcessingResponseMapper {
  public FileProcessingResponseDTO toFileProcessingResponseDTO(FileProcessingResponse fileProcessingResponse) {
    final StringValue errorMsg = fileProcessingResponse.getErrorMsg();
    return FileProcessingResponseDTO.builder()
        .filePath(fileProcessingResponse.getFilePath())
        .errorMessage(errorMsg.getValue())
        .fileProcessingStatus(FileProcessingStatus.valueOf(fileProcessingResponse.getStatus().toString()))
        .build();
  }

  public GitToHarnessProcessingResponseDTO toProcessingResponseDTO(ProcessingResponse processingResponse) {
    ProcessingFailureStage processingFailureStage = processingResponse.getProcessingFailureStage();
    List<FileProcessingResponse> responseList = processingResponse.getResponseList();
    List<FileProcessingResponseDTO> fileProcessingResponseDTOList =
        emptyIfNull(responseList)
            .stream()
            .map(ProcessingResponseMapper::toFileProcessingResponseDTO)
            .collect(Collectors.toList());
    GitToHarnessProcessingResponseDTOBuilder gitToHarnessProcessingResponseDTOBuilder =
        GitToHarnessProcessingResponseDTO.builder()
            .accountId(processingResponse.getAccountId())
            .fileResponses(fileProcessingResponseDTOList);
    if (processingFailureStage != null) {
      gitToHarnessProcessingResponseDTOBuilder.msvcProcessingFailureStage(
          MsvcProcessingFailureStage.valueOf(processingFailureStage.toString()));
    }
    return gitToHarnessProcessingResponseDTOBuilder.build();
  }
}
