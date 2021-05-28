package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("gitToHarnessProcessingStatus")
@TypeAlias("io.harness.gitsync.common.beans.GitToHarnessProcessingStatus")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "gitToHarnessProcessingStatus", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "GitToHarnessProcessingStatusKeys")
@OwnedBy(DX)
public class GitToHarnessProcessingStatus {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  @NotNull private String accountIdentifier;
  @NotNull private String changeSetId;
  @NotNull private String repoUrl;
  @NotNull private String branch;
  @NotNull private String eventType;
  @NotNull private GitToHarnessProcessingStepType stepType;
  @NotNull private GitToHarnessProcessingStepStatus stepStatus;
}
