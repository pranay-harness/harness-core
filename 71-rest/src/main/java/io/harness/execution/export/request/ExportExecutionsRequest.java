package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.execution.export.request.ExportExecutionsRequest.ExportExecutionsRequestKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.List;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "ExportExecutionsRequestKeys")
@Entity(value = "exportExecutionsRequests", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_status"),
      fields = { @Field(ExportExecutionsRequestKeys.accountId)
                 , @Field(ExportExecutionsRequestKeys.status) })
  ,
      @Index(options = @IndexOptions(name = "status_nextIteration"), fields = {
        @Field(ExportExecutionsRequestKeys.status), @Field(ExportExecutionsRequestKeys.nextIteration)
      }), @Index(options = @IndexOptions(name = "status_expiresAt_nextCleanupIteration"), fields = {
        @Field(ExportExecutionsRequestKeys.status)
        , @Field(ExportExecutionsRequestKeys.expiresAt), @Field(ExportExecutionsRequestKeys.nextCleanupIteration)
      })
})
public class ExportExecutionsRequest
    implements PersistentRegularIterable, UuidAware, CreatedAtAware, CreatedByAware, AccountAccess {
  public enum OutputFormat { JSON }
  public enum Status { QUEUED, READY, FAILED, EXPIRED }

  @Id private String uuid;

  @NonNull private String accountId;
  @NonNull private OutputFormat outputFormat;
  @NonNull private ExportExecutionsRequestQuery query;

  private boolean notifyOnlyTriggeringUser;
  private List<String> userGroupIds;

  @NonNull private Status status;
  @NonNull private long totalExecutions;
  @NonNull private long expiresAt;

  // For status = READY
  private String fileId;

  // For status = FAILED
  private String errorMessage;

  private long createdAt;
  private CreatedByType createdByType;
  private EmbeddedUser createdBy;

  private Long nextIteration;
  private Long nextCleanupIteration;

  public Long obtainNextIteration(String fieldName) {
    if (ExportExecutionsRequestKeys.nextCleanupIteration.equals(fieldName)) {
      return nextCleanupIteration;
    } else if (ExportExecutionsRequestKeys.nextIteration.equals(fieldName)) {
      return nextIteration;
    }

    throw new IllegalStateException(format("Unknown field name for iteration: %s", fieldName));
  }

  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (ExportExecutionsRequestKeys.nextCleanupIteration.equals(fieldName)) {
      this.nextCleanupIteration = nextIteration;
      return;
    } else if (ExportExecutionsRequestKeys.nextIteration.equals(fieldName)) {
      this.nextIteration = nextIteration;
      return;
    }

    throw new IllegalStateException(format("Unknown field name for iteration: %s", fieldName));
  }
}
