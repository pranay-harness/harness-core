package software.wings.beans.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;
import software.wings.alerts.AlertStatus;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

/**
 * Created by brett on 10/18/17
 */
@FieldNameConstants(innerTypeName = "AlertKeys")
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("appId"), @Field("type"), @Field("status") },
      options = @IndexOptions(name = "accountAppTypeStatusIdx"))
  ,
      @Index(fields = { @Field("type")
                        , @Field("createdAt") }, options = @IndexOptions(name = "createdAtTypeIndex"))
})
@Data
@Builder
@Entity(value = "alerts")
@HarnessEntity(exportable = false)
public class Alert
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, PersistentRegularIterable, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  @Indexed private String accountId;
  private AlertType type;
  private AlertStatus status;
  private String title;
  private String resolutionTitle;
  private AlertCategory category;
  private AlertSeverity severity;
  private AlertData alertData;
  private long closedAt;
  private int triggerCount;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(14).toInstant());

  @Indexed private Long cvCleanUpIteration;

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (AlertKeys.cvCleanUpIteration.equals(fieldName)) {
      this.cvCleanUpIteration = nextIteration;
      return;
    }
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (AlertKeys.cvCleanUpIteration.equals(fieldName)) {
      return this.cvCleanUpIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }
}
