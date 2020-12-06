package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlChangeSet;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by bsollish 10/13/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gitCommits", noClassnameStored = true)

@NgUniqueIndex(name = "gitCommitIdx", fields = { @Field(GitCommitKeys.accountId)
                                                 , @Field(GitCommitKeys.commitId) })
@CdIndex(name = "gitCommitStatusLastUpdatedIdx",
    fields =
    {
      @Field(GitCommitKeys.accountId)
      , @Field(GitCommitKeys.status), @Field(value = GitCommitKeys.lastUpdatedAt, type = IndexType.DESC)
    })
@CdIndex(name = "gitCommitAccountIdLastUpdatedAT",
    fields = { @Field(GitCommitKeys.accountId)
               , @Field(value = GitCommitKeys.lastUpdatedAt, type = IndexType.DESC) })
@CdIndex(name = "gitCommitAccountIdCreatedAtDesc",
    fields = { @Field(GitCommitKeys.accountId)
               , @Field(value = GitCommitKeys.createdAt, type = IndexType.DESC), })
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "GitCommitKeys")
public class GitCommit extends Base implements AccountAccess {
  private String accountId;
  private String yamlGitConfigId;
  private String commitId;
  private YamlChangeSet yamlChangeSet;
  private GitCommandResult gitCommandResult;
  @FdIndex private Status status;
  private FailureReason failureReason;
  private List<String> yamlChangeSetsProcessed;
  private List<String> yamlGitConfigIds;
  private GitFileProcessingSummary fileProcessingSummary;
  private String commitMessage;
  private String gitConnectorId;
  private String repositoryName;
  private String branchName;

  public enum Status { QUEUED, RUNNING, COMPLETED, FAILED, COMPLETED_WITH_ERRORS, SKIPPED }

  public enum FailureReason {
    GIT_CONNECTION_FAILED,
    GIT_CLONE_FAILED,
    GIT_PUSH_FAILED,
    GIT_PULL_FAILED,
    COMMIT_PARSING_FAILED
  }

  public static final List<Status> GIT_COMMIT_PROCESSED_STATUS =
      ImmutableList.of(Status.COMPLETED, Status.COMPLETED_WITH_ERRORS);

  public static final List<Status> GIT_COMMIT_ALL_STATUS_LIST = ImmutableList.<GitCommit.Status>builder()
                                                                    .addAll(GIT_COMMIT_PROCESSED_STATUS)
                                                                    .add(Status.FAILED)
                                                                    .add(Status.SKIPPED)
                                                                    .build();

  @UtilityClass
  public static final class GitCommitKeys {
    // Temporary
    public static final String lastUpdatedAt = "lastUpdatedAt";
    public static final String createdAt = "createdAt";
    public static final String gitFileChanges = "yamlChangeSet.gitFileChanges";
  }
}
