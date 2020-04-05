package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
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
@Getter
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "GitFileActivityKeys")
@Entity(value = "gitFileActivity")
@HarnessEntity(exportable = false)
public class GitFileActivity extends Base {
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

  public enum Status { SUCCESS, FAILED, DISCARDED, EXPIRED, SKIPPED, QUEUED }

  public enum TriggeredBy { USER, GIT, FULL_SYNC }
}