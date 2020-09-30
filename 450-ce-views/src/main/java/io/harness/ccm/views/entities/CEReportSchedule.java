package io.harness.ccm.views.entities;

import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;
import javax.validation.constraints.Size;

@NgUniqueIndex(name = "view_report_name", fields = { @Field("name")
                                                     , @Field("viewsId") })
@CdIndex(name = "account_enabled_type", fields = { @Field("accountId")
                                                   , @Field("enabled"), @Field("type") })
@Data
@Builder
@FieldNameConstants(innerTypeName = "CEReportScheduleKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceReportSchedule", noClassnameStored = true)
@StoreIn("events")
public class CEReportSchedule implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess,
                                         CreatedByAware, UpdatedByAware {
  @Id String uuid;
  @NotEmpty(message = "Name for report schedule must not be empty")
  @Size(min = 1, max = 32, message = "for report schedule must be between 1 and 32 characters long")
  String name;
  @Builder.Default boolean enabled = true;
  String description;
  @NotEmpty(message = "At least one ce viewId must be provided") String[] viewsId;
  @NotEmpty(message = "report schedule cron must not be empty") String userCron;
  @NotEmpty(message = "At least one email recipient must be provided") String[] recipients;
  String accountId;
  long createdAt;
  long lastUpdatedAt;
  EmbeddedUser createdBy;
  EmbeddedUser lastUpdatedBy;
  Date nextExecution;
}
