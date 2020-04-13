package software.wings.beans.marketplace.gcp;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAccess;
import io.harness.persistence.UuidAccess;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.jersey.JsonViews;

import java.time.Instant;
import javax.validation.constraints.NotNull;

@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("startTimestamp") },
    options = @IndexOptions(unique = true, name = "accountId_startTimestamp_unique_idx", background = true)))
@Value
@FieldNameConstants(innerTypeName = "GCPUsageReportKeys")
@Entity(value = "gcpUsageReport", noClassnameStored = true)
@HarnessEntity(exportable = true)

public class GCPUsageReport implements PersistentEntity, UuidAccess, CreatedAtAccess, UpdatedAtAccess, AccountAccess {
  @Id private String uuid;
  @NonFinal private String accountId;
  @NonFinal private String consumerId;
  @NonFinal private String operationId;
  @NonFinal private String entitlementName;
  @NonFinal private Instant startTimestamp;
  @NonFinal private Instant endTimestamp;
  @NonFinal private long instanceUsage;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;

  public GCPUsageReport(String accountId, String consumerId, String operationId, String entitlementName,
      Instant usageStartTime, Instant usageEndTime, long instanceUsage) {
    long currentMillis = Instant.now().toEpochMilli();
    this.uuid = String.format("%s-%s", accountId, usageStartTime.toEpochMilli());
    this.accountId = accountId;
    this.consumerId = consumerId;
    this.operationId = operationId;
    this.entitlementName = entitlementName;
    this.startTimestamp = usageStartTime;
    this.endTimestamp = usageEndTime;
    this.instanceUsage = instanceUsage;
    this.createdAt = currentMillis;
    this.lastUpdatedAt = currentMillis;
  }
}
