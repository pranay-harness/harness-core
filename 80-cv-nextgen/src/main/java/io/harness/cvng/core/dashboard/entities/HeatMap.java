package io.harness.cvng.core.dashboard.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "values", "deeplinkMetadata", "deeplinkUrl"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "HeatMapKeys")
@Entity(value = "heatMaps", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class HeatMap implements UuidAware, CreatedAtAware, AccountAccess, PersistentEntity {
  @Id private String uuid;
  @FdIndex private String serviceIdentifier;
  @FdIndex private String envIdentifier;
  @FdIndex private CVMonitoringCategory category;
  @FdIndex private Instant heatMapBucketStartTime;
  @FdIndex private String accountId;
  private Set<HeatMapRisk> heatMapRisks;

  private long createdAt;

  @JsonIgnore
  @SchemaIgnore
  @Builder.Default
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "HeatMapRiskKeys")
  @EqualsAndHashCode(of = {"timeStamp"})
  public static class HeatMapRisk implements Comparable<HeatMapRisk> {
    private Instant timeStamp;
    private double riskScore;

    @Override
    public int compareTo(HeatMapRisk o) {
      return this.timeStamp.compareTo(o.timeStamp);
    }
  }
}
