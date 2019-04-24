package software.wings.beans.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
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
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
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
@Data
@Builder
@Entity(value = "alerts")
public class Alert implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore @Indexed private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  @Indexed private String accountId;
  @Indexed private AlertType type;
  @Indexed private AlertStatus status;
  private String title;
  private AlertCategory category;
  private AlertSeverity severity;
  private AlertData alertData;
  private long closedAt;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
