package io.harness.cvng.dashboard.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Set;

@CdIndex(name = "insertionIdx",
    fields =
    {
      @Field("serviceIdentifier")
      , @Field("envIdentifier"), @Field("category"), @Field("heatMapResolution"), @Field("heatMapBucketStartTime"),
          @Field("heatMapBucketEndTime")
    })
@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "HeatMapKeys")
@Entity(value = "heatMaps", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class HeatMap implements UuidAware, CreatedAtAware, AccountAccess, PersistentEntity {
  @Id private String uuid;
  private String serviceIdentifier;
  private String envIdentifier;
  @FdIndex private CVMonitoringCategory category;
  private HeatMapResolution heatMapResolution;
  @FdIndex private Instant heatMapBucketStartTime;
  @FdIndex private Instant heatMapBucketEndTime;
  @FdIndex private String accountId;
  private Set<HeatMapRisk> heatMapRisks;

  @FdIndex private long createdAt;

  @JsonIgnore
  @SchemaIgnore
  @Builder.Default
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "HeatMapRiskKeys")
  @EqualsAndHashCode(of = {"startTime", "endTime"})
  public static class HeatMapRisk implements Comparable<HeatMapRisk> {
    private Instant startTime;
    private Instant endTime;
    private double riskScore;

    @Override
    public int compareTo(HeatMapRisk o) {
      return this.startTime.compareTo(o.startTime);
    }
  }

  public enum HeatMapResolution {
    FIVE_MIN(Duration.ofMinutes(5), Duration.ofHours(4)),
    FIFTEEN_MINUTES(Duration.ofMinutes(15), Duration.ofHours(12)),
    THIRTY_MINUTES(Duration.ofMinutes(30), Duration.ofDays(1)),
    FOUR_HOURS(Duration.ofHours(4), Duration.ofDays(7)),
    TWELVE_HOURS(Duration.ofHours(12), Duration.ofDays(30));

    private Duration resolution;
    private Duration bucketSize;
    HeatMapResolution(Duration resolution, Duration bucketSize) {
      this.resolution = resolution;
      this.bucketSize = bucketSize;
    }

    public Duration getResolution() {
      return resolution;
    }

    public Duration getBucketSize() {
      return bucketSize;
    }

    public static HeatMapResolution getHeatMapResolution(Instant startTime, Instant endTime) {
      for (HeatMapResolution heatMapResolution : HeatMapResolution.values()) {
        if (endTime.toEpochMilli() - startTime.toEpochMilli() <= heatMapResolution.getBucketSize().toMillis()) {
          return heatMapResolution;
        }
      }
      return HeatMapResolution.TWELVE_HOURS;
    }
  }
}
