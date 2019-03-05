package software.wings.beans;

import io.harness.annotation.HarnessExportableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.yaml.GitCommandResult;
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
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("commitId") },
    options = @IndexOptions(name = "gitCommitIdx", unique = true, dropDups = true)))
@HarnessExportableEntity
public class GitCommit extends Base {
  public static final String YAML_GIT_CONFIG_ID_KEY = "yamlGitConfigId";
  public static final String YAML_GIT_CONFIG_IDS_KEY = "yamlGitConfigIds";
  public static final String STATUS_KEY = "status";

  private String accountId;
  private String yamlGitConfigId;
  private String commitId;
  private YamlChangeSet yamlChangeSet;
  private GitCommandResult gitCommandResult;
  private Status status;
  private FailureReason failureReason;
  private List<String> yamlChangeSetsProcessed;
  private List<String> yamlGitConfigIds;

  public enum Status { QUEUED, RUNNING, COMPLETED, FAILED }

  public enum FailureReason {
    GIT_CONNECTION_FAILED,
    GIT_CLONE_FAILED,
    GIT_PUSH_FAILED,
    GIT_PULL_FAILED,
    COMMIT_PARSING_FAILED
  }
}
