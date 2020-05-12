package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.yaml.Change.ChangeType;
/**
 * @author vardanb
 */
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("processingCommitId"), @Field("filePath"), @Field("status") },
      options = @IndexOptions(background = true, name = "accountId_procCommitId_filePath_status"))
  ,
      @Index(fields = {
        @Field("accountId"), @Field("processingCommitId"), @Field("status")
      }, options = @IndexOptions(background = true, name = "accountId_procCommitId_status")), @Index(fields = {
        @Field("accountId"), @Field("filePath")
      }, options = @IndexOptions(background = true, name = "accountId_filePath")),
})
@Data
@Builder
@FieldNameConstants(innerTypeName = "GitFileActivityKeys")
@Entity(value = "gitFileActivity")
@HarnessEntity(exportable = false)
public class GitFileActivity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  private String accountId;
  private String filePath;
  private String fileContent;
  private String commitId;
  private String processingCommitId;
  private ChangeType changeType;
  private String errorMessage;
  private Status status;
  private TriggeredBy triggeredBy;
  private boolean changeFromAnotherCommit;
  private String commitMessage;
  private String appId;
  private long createdAt;
  private long lastUpdatedAt;
  private String gitConnectorId;
  private String branchName;
  @Transient private String connectorName;

  public enum Status { SUCCESS, FAILED, DISCARDED, EXPIRED, SKIPPED, QUEUED }

  public enum TriggeredBy { USER, GIT, FULL_SYNC }
}