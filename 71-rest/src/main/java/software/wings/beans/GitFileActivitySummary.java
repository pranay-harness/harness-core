package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.yaml.gitSync.GitFileProcessingSummary;

import javax.validation.constraints.NotNull;

/**
 * Created by deepak 5/09/20
 */
@Data
@Builder
@AllArgsConstructor
@Entity(value = "gitFileActivitySummary", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "GitFileActivitySummaryKeys")
public class GitFileActivitySummary implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @NotNull(groups = {Update.class}) String uuid;
  private String accountId;
  private String commitId;
  private String branchName;
  private String gitConnectorId;
  private String appId;
  private long createdAt;
  private String commitMessage;
  private long lastUpdatedAt;
  private Boolean gitToHarness;
  private GitCommit.Status status;
  private GitFileProcessingSummary fileProcessingSummary;
}
