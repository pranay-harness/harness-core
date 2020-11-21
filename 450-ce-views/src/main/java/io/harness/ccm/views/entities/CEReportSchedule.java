package io.harness.ccm.views.entities;

import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import java.util.Date;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@CdIndex(name = "account_enabled_type",
    fields = { @Field("accountId")
               , @Field("enabled"), @Field("name"), @Field("viewsId"), @Field("type") })
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
  @Size(min = 1, max = 32, message = ": for report schedule name must be between 1 and 32 characters long")
  String name;
  @Builder.Default boolean enabled = true;
  @Size(max = 100, message = ": for report schedule description must be between 0 and 100 characters long")
  @Builder.Default
  String description = "";
  @NotNull @Size(min = 1, max = 1, message = ": for report schedule, one viewId is needed") String[] viewsId;
  @NotEmpty(message = "report schedule cron must not be empty") String userCron;
  @Size(max = 50, message = ": for report schedule maximum 50 recipients are allowed")
  @NotEmpty(message = "At least one email recipient must be provided")
  String[] recipients;
  String accountId;
  long createdAt;
  long lastUpdatedAt;
  EmbeddedUser createdBy;
  EmbeddedUser lastUpdatedBy;
  Date nextExecution;
}
