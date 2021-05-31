package io.harness.gitsync.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "GitProcessingRequestKeys")
@Document("gitProcessRequestSdk")
@TypeAlias("io.harness.gitsync.beans.GitProcessRequest")
@Entity(value = "gitProcessRequestSdk", noClassnameStored = true)
@OwnedBy(DX)
public class GitProcessRequest {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotNull String commitId;
  List<FileStatus> fileStatuses;
  @NotNull String accountId;
  @NotNull String repoUrl;
  @NotNull String branch;

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "FileStatusKeys")
  @ToString(exclude = "fileContent")
  public static class FileStatus {
    String filePath;
    String fileContent;
    String changeType;
    String entityType;
    FileProcessStatus status;
    String errorMessage;
  }

  public enum FileProcessStatus { UNPROCESSED, SUCCESS, FAILURE, SKIPPED }
}
