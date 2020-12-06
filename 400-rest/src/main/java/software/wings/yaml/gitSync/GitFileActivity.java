package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import io.harness.git.model.ChangeType;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import software.wings.beans.GitRepositoryInfo;

import javax.ws.rs.DefaultValue;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

/**
 * @author vardanb
 */

@CdIndex(name = "accountId_procCommitId_filePath_status",
    fields = { @Field("accountId")
               , @Field("processingCommitId"), @Field("filePath"), @Field("status") })
@CdIndex(name = "accountId_procCommitId_status",
    fields = { @Field("accountId")
               , @Field("processingCommitId"), @Field("status") })
@CdIndex(name = "accountId_filePath", fields = { @Field("accountId")
                                                 , @Field("filePath") })
@CdIndex(name = "accountId_commitId_Idx", fields = { @Field("accountId")
                                                     , @Field("commitId") })
@CdIndex(name = "accountId_appId_createdAt_Idx",
    fields = { @Field("accountId")
               , @Field("appId"), @Field(value = "createdAt", type = IndexType.DESC) })
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
  private String repositoryName;
  private String branchName;
  @Transient private String connectorName;
  @Transient private GitRepositoryInfo repositoryInfo;
  @Transient @DefaultValue("false") private boolean userDoesNotHavePermForFile;

  public enum Status { SUCCESS, FAILED, DISCARDED, EXPIRED, SKIPPED, QUEUED }

  public enum TriggeredBy { USER, GIT, FULL_SYNC }
}
