package io.harness.ccm.billing.entities;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "cloudBillingTransferRuns", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CloudBillingTransferRunKeys")
public final class CloudBillingTransferRun implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware {
  @Id String uuid;
  private String accountId;
  private String organizationUuid;
  private String billingDataPipelineRecordId;
  private String transferRunResourceName;
  private TransferJobRunState state;
  long lastUpdatedAt;
}
