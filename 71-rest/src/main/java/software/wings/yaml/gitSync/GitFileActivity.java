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
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("filePath"), @Field("commitId") },
    options = @IndexOptions(unique = true, name = "uniqueIdx")))
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
  private ChangeType changeType;
  private String errorMessage;
  private Status status;
  private TriggeredBy triggeredBy;

  public enum Status { SUCCESS, FAILED, DISCARDED, EXPIRED, SKIPPED }

  public enum TriggeredBy { USER, GIT, FULL_SYNC }
}