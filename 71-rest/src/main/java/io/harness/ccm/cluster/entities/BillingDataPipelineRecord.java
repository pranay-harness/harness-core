package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
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
@Entity(value = "billingDataPipelineRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "BillingDataPipelineRecordKeys")
public class BillingDataPipelineRecord implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id private String uuid;
  long createdAt;

  private String accountId;
  private String settingId;
  private String masterAccountId;
  private String accountName;

  private String dataSetId;
  private String dataTransferJobName;
  private String fallbackTableScheduledQueryName;
  private String preAggregatedScheduledQueryName;
}
