package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("gitToHarnessProgress")
@TypeAlias("io.harness.gitsync.common.beans.GitToHarnessProcessingProgress")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "gitToHarnessProgress", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "GitToHarnessProgressKeys")
@OwnedBy(DX)
public class GitToHarnessProgress {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  @NotNull private String accountIdentifier;
  @FdIndex @NotNull private String yamlChangeSetId;
  @NotNull private String repoUrl;
  @NotNull private String branch;
  @NotNull private String commitId;
  @NotNull private YamlChangeSetEventType eventType;
  @NotNull private GitToHarnessProcessingStepType stepType;
  @NotNull private GitToHarnessProcessingStepStatus stepStatus;
  @NotNull private Long stepStartingTime;
  @NotNull private GitToHarnessProgressStatus gitToHarnessProgressStatus;
  private String processingCommitId;
  List<GitToHarnessFileProcessingRequest> gitFileChanges;
  List<GitToHarnessProcessingResponseDTO> processingResponse;
  @Version Long version;
  @EqualsAndHashCode.Exclude @CreatedDate private long createdAt;
  @EqualsAndHashCode.Exclude @LastModifiedDate private long lastUpdatedAt;
}
