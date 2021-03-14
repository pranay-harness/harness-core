package io.harness.ccm.communication.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CESlackWebhookKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceSlackWebhooks", noClassnameStored = true)
@TargetModule(Module._490_CE_COMMONS)
public class CESlackWebhook implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @NotBlank String accountId;
  String webhookUrl;
  boolean sendCostReport;
  boolean sendAnomalyAlerts;
  long createdAt;
  long lastUpdatedAt;
}
