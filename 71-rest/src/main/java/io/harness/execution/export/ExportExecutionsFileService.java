package io.harness.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.ExportExecutionsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FileMetadata;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ExportExecutionsFileService {
  private static final FileBucket FILE_BUCKET = FileBucket.EXPORT_EXECUTIONS;

  @Inject private FileService fileService;

  public String uploadFile(@NotNull String accountId, @NotNull File file) {
    try (FileInputStream in = new FileInputStream(file)) {
      // TODO(gpahal): Try to save file with an expiration.
      return fileService.saveFile(prepareFileMetadata(accountId, file), in, FILE_BUCKET);
    } catch (Exception ex) {
      throw new ExportExecutionsException("Unable to upload file for export executions request", ex);
    }
  }

  @VisibleForTesting
  FileMetadata prepareFileMetadata(@NotNull String accountId, @NotNull File file) {
    return FileMetadata.builder()
        .accountId(accountId)
        .fileName(file.getName())
        .fileUuid(UUIDGenerator.generateUuid())
        .build();
  }

  public void downloadFileToStream(@NotNull String fileId, @NotNull OutputStream outputStream) {
    try {
      fileService.downloadToStream(fileId, outputStream, FILE_BUCKET);
    } catch (Exception ex) {
      throw new ExportExecutionsException("Unable to download file for export executions request", ex);
    }
  }

  public void deleteFile(String fileId) {
    if (fileId == null) {
      return;
    }

    try {
      fileService.deleteFile(fileId, FILE_BUCKET);
    } catch (Exception ex) {
      throw new ExportExecutionsException(
          format("Unable to delete file [%s] for export executions request", fileId), ex);
    }
  }
}
