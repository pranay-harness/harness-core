package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.yaml.gitSync.YamlChangeSet;

/**
 * Created by bsollish 10/13/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "gitCommits", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("commitId") },
    options = @IndexOptions(name = "gitCommitIdx", unique = true, dropDups = true)))
public class GitCommit extends Base {
  private String accountId;
  private String yamlGitConfigId;
  private String commitId;
  private YamlChangeSet yamlChangeSet;
  private GitCommandResult gitCommandResult;
  private Status status = Status.QUEUED;
  private FailureReason failureReason;

  public enum Status { QUEUED, RUNNING, COMPLETED, FAILED }

  public enum FailureReason {
    GIT_CONNECTION_FAILED,
    GIT_CLONE_FAILED,
    GIT_PUSH_FAILED,
    GIT_PULL_FAILED,
    COMMIT_PARSING_FAILED
  }
}
