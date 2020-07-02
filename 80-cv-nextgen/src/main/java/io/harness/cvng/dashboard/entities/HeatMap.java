package io.harness.cvng.dashboard.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;

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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@CdIndex(name = "insertionIdx",
    fields =
    {
      @Field("serviceIdentifier")
      , @Field("envIdentifier"), @Field("category"), @Field("heatMapResolution"), @Field("heatMapBucketStartTime")
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
  @EqualsAndHashCode(of = {"timeStamp"})
  public static class HeatMapRisk implements Comparable<HeatMapRisk> {
    private Instant timeStamp;
    private double riskScore;

    @Override
    public int compareTo(HeatMapRisk o) {
      return this.timeStamp.compareTo(o.timeStamp);
    }
  }

  public enum HeatMapResolution {
    FIVE_MIN(CV_ANALYSIS_WINDOW_MINUTES, TimeUnit.HOURS.toMinutes(4)),
    FIFTEEN_MINUTES(CV_ANALYSIS_WINDOW_MINUTES * 3, TimeUnit.HOURS.toMinutes(12)),
    THIRTY_MINUTES(CV_ANALYSIS_WINDOW_MINUTES * 6, TimeUnit.DAYS.toMinutes(1)),
    FOUR_HOURS(TimeUnit.HOURS.toMinutes(4), TimeUnit.DAYS.toMinutes(7)),
    TWELVE_HOURS(TimeUnit.HOURS.toMinutes(12), TimeUnit.DAYS.toMinutes(30));

    private long resolutionMinutes;
    private long bucketSizeMinutes;
    HeatMapResolution(long resolutionMinutes, long bucketSizeMinutes) {
      this.resolutionMinutes = resolutionMinutes;
      this.bucketSizeMinutes = bucketSizeMinutes;
    }

    public long getResolutionMinutes() {
      return resolutionMinutes;
    }

    public long getBucketSizeMinutes() {
      return bucketSizeMinutes;
    }
  }
}
