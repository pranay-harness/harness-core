package software.wings.beans;

import com.google.common.collect.ImmutableList;

import io.harness.annotation.HarnessEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlChangeSet;

import java.util.List;

/**
 * Created by bsollish 10/13/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gitCommits", noClassnameStored = true)
@Indexes({
  @Index(fields = { @Field(GitCommitKeys.accountId)
                    , @Field(GitCommitKeys.commitId) },
      options = @IndexOptions(name = "gitCommitIdx", unique = true, dropDups = true))
  ,
      @Index(fields = {
        @Field(GitCommitKeys.accountId)
        , @Field(GitCommitKeys.status), @Field(value = GitCommitKeys.lastUpdatedAt, type = IndexType.DESC)
      }, options = @IndexOptions(name = "gitCommitStatusLastUpdatedIdx")), @Index(fields = {
        @Field(GitCommitKeys.accountId), @Field(value = GitCommitKeys.lastUpdatedAt, type = IndexType.DESC)
      }, options = @IndexOptions(name = "gitCommitAccountIdLastUpdatedAT"))
})
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "GitCommitKeys")
public class GitCommit extends Base {
  private String accountId;
  private String yamlGitConfigId;
  private String commitId;
  private YamlChangeSet yamlChangeSet;
  private GitCommandResult gitCommandResult;
  @Indexed private Status status;
  private FailureReason failureReason;
  private List<String> yamlChangeSetsProcessed;
  private List<String> yamlGitConfigIds;
  private GitFileProcessingSummary fileProcessingSummary;
  private String commitMessage;
  private String gitConnectorId;
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
