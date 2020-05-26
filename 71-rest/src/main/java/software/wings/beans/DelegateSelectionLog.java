package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.DelegateSelectionLog.DelegateSelectionLogKeys;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Set;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateSelectionLogRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "DelegateSelectionLogKeys")
@Indexes(@Index(options = @IndexOptions(name = "selectionLogsGroup", unique = true),
    fields =
    {
      @Field(value = DelegateSelectionLogKeys.accountId)
      , @Field(value = DelegateSelectionLogKeys.taskId), @Field(value = DelegateSelectionLogKeys.message),
          @Field(value = DelegateSelectionLogKeys.groupId)
    }))
public class DelegateSelectionLog implements PersistentEntity, UuidAware, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotEmpty private String accountId;
  @NotEmpty private Set<String> delegateIds;
  @NotEmpty private String taskId;
  @NotEmpty private String message;
  @NotEmpty private String conclusion;
  @NotEmpty private long eventTimestamp;
  /*
   * Used for deduplication of logs. Standalone logs will have a unique value and groups will have fixed.
   * */
  @NotEmpty private String groupId;

  @Builder.Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
