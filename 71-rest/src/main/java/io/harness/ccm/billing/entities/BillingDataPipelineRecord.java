package io.harness.ccm.billing.entities;

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

import java.time.Instant;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "billingDataPipelineRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "BillingDataPipelineRecordKeys")
public final class BillingDataPipelineRecord implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id private String uuid;
  long createdAt;

  private String accountId;
  private String settingId;
  private String accountName;
  private String cloudProvider;

  private String dataSetId;
  private String dataTransferJobName;
  private String transferJobResourceName;
  private String preAggregatedScheduledQueryName;
  private String preAggregatedScheduleQueryResourceName;

  private String gcpBqProjectId;
  private String gcpBqDatasetId;
  private String awsMasterAccountId;
  private String awsFallbackTableScheduledQueryName;

  private String dataTransferJobStatus;
  private String preAggregatedScheduledQueryStatus;
  private String awsFallbackTableScheduledQueryStatus;
  private Instant lastSuccessfulS3Sync;

  private List<String> awsLinkedAccountsToExclude;
  private List<String> awsLinkedAccountsToInclude;
}
