package io.harness.ccm.config;

import io.harness.ccm.config.GcpBillingAccount.GcpBillingAccountKeys;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
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
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GcpBillingAccountKeys")
@Entity(value = "gcpBillingAccount", noClassnameStored = true)
@NgUniqueIndex(name = "no_dup",
    fields = { @Field(GcpBillingAccountKeys.accountId)
               , @Field(GcpBillingAccountKeys.organizationSettingId) })
public class GcpBillingAccount implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  String organizationSettingId;
  String gcpBillingAccountId;
  String gcpBillingAccountName;
  boolean exportEnabled;
  String bqProjectId;
  String bqDatasetId;
  String bqDataSetRegion;

  long createdAt;
  long lastUpdatedAt;
}
