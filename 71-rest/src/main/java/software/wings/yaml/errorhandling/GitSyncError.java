package software.wings.yaml.errorhandling;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

/**
 * @author rktummala on 12/15/17
 */
@Entity(value = "gitSyncError")
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("yamlFilePath") },
    options = @IndexOptions(unique = true, name = "uniqueIdx")))
@Data
@EqualsAndHashCode(callSuper = true)
public class GitSyncError extends Base {
  private String accountId;
  private String yamlFilePath;
  private String yamlContent;
  private String gitCommitId;
  private String changeType;
  private String failureReason;
  private boolean fullSyncPath;

  @Builder
  public GitSyncError(String accountId, String yamlFilePath, String yamlContent, String gitCommitId, String changeType,
      String failureReason, boolean fullSyncPath) {
    this.accountId = accountId;
    this.yamlFilePath = yamlFilePath;
    this.yamlContent = yamlContent;
    this.gitCommitId = gitCommitId;
    this.changeType = changeType;
    this.failureReason = failureReason;
    this.fullSyncPath = fullSyncPath;
  }
}
