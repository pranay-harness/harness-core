package software.wings.yaml.errorhandling;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.service.impl.yaml.GitSyncErrorStatus;
import software.wings.yaml.errorhandling.GitToHarnessErrorDetails.GitToHarnessErrorDetailsKeys;
import software.wings.yaml.errorhandling.HarnessToGitErrorDetails.HarnessToGitErrorDetailsKeys;

/**
 * @author rktummala on 12/15/17
 */
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("yamlFilePath"), @Field("gitSyncDirection") },
      options = @IndexOptions(unique = true, background = true, name = "account_filepath_direction_idx"))
  ,
      @Index(fields = {
        @Field("accountId"), @Field("gitConnectorId"), @Field("branchName"), @Field("gitSyncDirection")
      }, options = @IndexOptions(background = true, name = "git_repo_selection_idx")), @Index(fields = {
        @Field("accountId"), @Field("gitSyncDirection"), @Field("additionalErrorDetails.gitCommitId")
      }, options = @IndexOptions(background = true, name = "gitCommitId_idx")), @Index(fields = {
        @Field("accountId"), @Field("gitSyncDirection"), @Field("additionalErrorDetails.previousCommitIdsWithError")
      }, options = @IndexOptions(background = true, name = "previousErrors_idx"))
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "GitSyncErrorKeys")
@Entity(value = "gitSyncError")
@HarnessEntity(exportable = false)
public class GitSyncError extends Base implements PersistentRegularIterable {
  private String accountId;
  private String yamlFilePath;
  private String changeType;
  private String failureReason;
  @Deprecated private String yamlContent;
  @Deprecated private String gitCommitId;
  @Setter @Indexed private Long nextIteration;
  // TODO @deepak All other fields of this collection will be marked depreceated and will no longer be in db, but
  // fullSyncPath variable will be there in db as it is boolean, so will need one more migration to remove it
  private boolean fullSyncPath;
  @Deprecated private String lastAttemptedYaml;
  private GitSyncErrorStatus status;
  private String gitConnectorId;
  @Transient private String gitConnectorName;
  private String branchName;
  private String yamlGitConfigId;
  @Deprecated private Long commitTime;
  private GitSyncErrorDetails additionalErrorDetails;
  private String gitSyncDirection;

  @Builder
  public GitSyncError(String accountId, String yamlFilePath, String changeType, String failureReason,
      String gitConnectorId, String branchName, String yamlGitConfigId, GitSyncErrorDetails additionalErrorDetails,
      String gitSyncDirection, Long commitTime, String lastAttemptedYaml, boolean fullSyncPath, String yamlContent,
      String gitCommitId, GitSyncErrorStatus status) {
    this.accountId = accountId;
    this.yamlFilePath = yamlFilePath;
    this.changeType = changeType;
    this.failureReason = failureReason;
    this.status = GitSyncErrorStatus.ACTIVE;
    this.gitConnectorId = gitConnectorId;
    this.branchName = branchName;
    this.yamlGitConfigId = yamlGitConfigId;
    this.additionalErrorDetails = additionalErrorDetails;
    this.gitSyncDirection = gitSyncDirection;
    this.commitTime = commitTime;
    this.lastAttemptedYaml = lastAttemptedYaml;
    this.fullSyncPath = fullSyncPath;
    this.yamlContent = yamlContent;
    this.gitCommitId = gitCommitId;
    this.status = status;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @UtilityClass
  public static final class GitSyncErrorKeys {
    public static final String gitCommitId =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.gitCommitId;
    public static final String commitTime =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitTime;
    public static final String fullSyncPath =
        GitSyncErrorKeys.additionalErrorDetails + "." + HarnessToGitErrorDetailsKeys.fullSyncPath;
    public static final String previousCommitIds =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.previousCommitIdsWithError;
  }

  public enum GitSyncDirection { GIT_TO_HARNESS, HARNESS_TO_GIT }
}
