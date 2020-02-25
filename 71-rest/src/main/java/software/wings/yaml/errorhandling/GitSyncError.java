package software.wings.yaml.errorhandling;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.service.impl.yaml.GitSyncErrorStatus;

/**
 * @author rktummala on 12/15/17
 */
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("yamlFilePath") },
    options = @IndexOptions(unique = true, name = "uniqueIdx")))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "GitSyncErrorKeys")
@Entity(value = "gitSyncError")
@HarnessEntity(exportable = false)
public class GitSyncError extends Base {
  private String accountId;
  private String yamlFilePath;
  private String yamlContent;
  private String gitCommitId;
  private String changeType;
  private String failureReason;
  private boolean fullSyncPath;
  private String lastAttemptedYaml;
  private GitSyncErrorStatus status;

  @Builder
  public GitSyncError(String accountId, String yamlFilePath, String yamlContent, String gitCommitId, String changeType,
      String failureReason, boolean fullSyncPath, String lastAttemptedYaml) {
    this.accountId = accountId;
    this.yamlFilePath = yamlFilePath;
    this.yamlContent = yamlContent;
    this.gitCommitId = gitCommitId;
    this.changeType = changeType;
    this.failureReason = failureReason;
    this.fullSyncPath = fullSyncPath;
    this.lastAttemptedYaml = lastAttemptedYaml;
    this.status = GitSyncErrorStatus.ACTIVE;
  }
}
