package software.wings.beans.alert;

import io.harness.alert.AlertData;
import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;
import software.wings.alerts.AlertStatus;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertReconciliation.AlertReconciliationKeys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "AlertKeys")

@CdIndex(name = "accountAppTypeStatusIdx",
    fields = { @Field(AlertKeys.accountId)
               , @Field(AlertKeys.appId), @Field(AlertKeys.type), @Field(AlertKeys.status) })
@CdIndex(name = "accountTypeStatusIdx",
    fields = { @Field(AlertKeys.accountId)
               , @Field(AlertKeys.type), @Field(AlertKeys.status) })
@CdIndex(name = "createdAtTypeIndex", fields = { @Field(AlertKeys.type)
                                                 , @Field(AlertKeys.createdAt) })
@CdIndex(name = "reconciliationIterator",
    fields =
    {
      @Field(AlertKeys.status)
      , @Field(AlertKeys.alertReconciliation + "." + AlertReconciliationKeys.needed),
          @Field(AlertKeys.alertReconciliation + "." + AlertReconciliationKeys.nextIteration)
    })
@Data
@Builder
@Entity(value = "alerts")
@HarnessEntity(exportable = false)
public class Alert
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, PersistentRegularIterable, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FdIndex @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  private String accountId;
  private AlertType type;
  private AlertStatus status;
  private String title;
  private String resolutionTitle;
  private AlertCategory category;
  private AlertSeverity severity;
  private AlertData alertData;
  private AlertReconciliation alertReconciliation;
  private long closedAt;
  private int triggerCount;
  private long lastTriggeredAt;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(14).toInstant());

  @FdIndex private Long cvCleanUpIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (AlertKeys.cvCleanUpIteration.equals(fieldName)) {
      this.cvCleanUpIteration = nextIteration;
      return;
    }
    if (AlertKeys.alertReconciliation_nextIteration.equals(fieldName)) {
      this.alertReconciliation.setNextIteration(nextIteration);
      return;
    }
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (AlertKeys.cvCleanUpIteration.equals(fieldName)) {
      return this.cvCleanUpIteration;
    }
    if (AlertKeys.alertReconciliation_nextIteration.equals(fieldName)) {
      return this.alertReconciliation.getNextIteration();
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @UtilityClass
  public static final class AlertKeys {
    public static final String alertReconciliation_needed =
        AlertKeys.alertReconciliation + "." + AlertReconciliationKeys.needed;
    public static final String alertReconciliation_nextIteration =
        alertReconciliation + "." + AlertReconciliationKeys.nextIteration;
  }
}
