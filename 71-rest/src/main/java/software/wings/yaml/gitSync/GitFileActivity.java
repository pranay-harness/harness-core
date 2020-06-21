package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.Indexes;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.yaml.Change.ChangeType;

import javax.ws.rs.DefaultValue;

/**
 * @author vardanb
 */
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("processingCommitId"), @Field("filePath"), @Field("status") },
      name = "accountId_procCommitId_filePath_status")
  ,
      @Index(fields = { @Field("accountId")
                        , @Field("processingCommitId"), @Field("status") },
          name = "accountId_procCommitId_status"),
      @Index(fields = { @Field("accountId")
                        , @Field("filePath") }, name = "accountId_filePath"),
      @Index(fields = { @Field("accountId")
                        , @Field("commitId") }, name = "accountId_commitId_Idx")
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
  private String processingCommitMessage;
  private String appId;
  private long createdAt;
  private long lastUpdatedAt;
  private String gitConnectorId;
  private String branchName;
  @Transient private String connectorName;
  @Transient @DefaultValue("false") private boolean userDoesNotHavePermForFile;

  public enum Status { SUCCESS, FAILED, DISCARDED, EXPIRED, SKIPPED, QUEUED }

  public enum TriggeredBy { USER, GIT, FULL_SYNC }
}