package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import software.wings.yaml.gitSync.GitFileProcessingSummary;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by deepak 5/09/20
 */
@Data
@Builder
@AllArgsConstructor
@Entity(value = "gitFileActivitySummary", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "GitFileActivitySummaryKeys")
@HarnessEntity(exportable = true)
public final class GitFileActivitySummary
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("gitCommits_for_appId_indx")
                 .field(GitFileActivitySummaryKeys.accountId)
                 .field(GitFileActivitySummaryKeys.appId)
                 .field(GitFileActivitySummaryKeys.gitToHarness)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("gitCommits_createdAt_direction_indx")
                 .field(GitFileActivitySummaryKeys.accountId)
                 .field(GitFileActivitySummaryKeys.createdAt)
                 .field(GitFileActivitySummaryKeys.gitToHarness)
                 .build())
        .build();
  }

  @Id @NotNull(groups = {Update.class}) String uuid;
  private String accountId;
  private String commitId;
  private String branchName;
  private String repositoryName;
  private String gitConnectorId;
  private String appId;
  private long createdAt;
  private String commitMessage;
  private long lastUpdatedAt;
  private Boolean gitToHarness;
  private GitCommit.Status status;
  private GitFileProcessingSummary fileProcessingSummary;
  @Transient private String connectorName;
  @Transient private GitRepositoryInfo repositoryInfo;
}
