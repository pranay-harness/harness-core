package io.harness.cvng.servicelevelobjective.entities;

import static java.time.temporal.ChronoUnit.DAYS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.servicelevelobjective.beans.DayOfWeek;
import io.harness.cvng.servicelevelobjective.beans.SLOCalenderType;
import io.harness.cvng.servicelevelobjective.beans.SLOTargetType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ServiceLevelObjectiveKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "serviceLevelObjectives", noClassnameStored = true)
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.CV)
@StoreIn(DbAliases.CVNG)
public class ServiceLevelObjective
    implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware, CreatedAtAware {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  @Id private String uuid;
  String identifier;
  String name;
  String desc;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  String userJourneyIdentifier;
  String healthSourceIdentifier;
  String monitoredServiceIdentifier;
  List<String> serviceLevelIndicators;
  SLOTarget sloTarget;
  private long lastUpdatedAt;
  private long createdAt;
  private Double sloTargetPercentage;
  public ZoneOffset getZoneOffset() {
    return ZoneOffset.UTC; // hardcoding it to UTC for now. We need to ask it from user.
  }

  public int getTotalErrorBudgetMinutes(LocalDate currentDate) {
    int currentWindowMinutes = getCurrentTimeRange(currentDate).totalMinutes(getZoneOffset());
    Double errorBudgetPercentage = getSloTargetPercentage();
    return (int) Math.round(((100 - errorBudgetPercentage) * currentWindowMinutes) / 100);
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_query_idx")
                 .unique(true)
                 .field(ServiceLevelObjectiveKeys.accountId)
                 .field(ServiceLevelObjectiveKeys.orgIdentifier)
                 .field(ServiceLevelObjectiveKeys.projectIdentifier)
                 .field(ServiceLevelObjectiveKeys.identifier)
                 .build())
        .build();
  }

  public TimePeriod getCurrentTimeRange(LocalDate currentDate) {
    return sloTarget.getCurrentTimeRange(currentDate);
  }

  @Value
  @Builder
  public static class TimePeriod {
    LocalDate startDate;
    LocalDate endDate;

    public Period getRemainingDays(LocalDate currentDate) {
      return Period.between(currentDate, endDate);
    }
    public int getTotalDays() {
      return (int) DAYS.between(startDate, endDate);
    }
    public int totalMinutes(ZoneId zoneId) {
      return (int) Duration.between(getStartTime(zoneId), getEndTime(zoneId)).toMinutes();
    }

    /**
     * Start time is inclusive.
     */
    public Instant getStartTime(ZoneId zoneId) {
      return getStartDate().atStartOfDay(zoneId).toInstant();
    }

    /**
     * End time is exclusive.
     */
    public Instant getEndTime(ZoneId zoneId) {
      // this will end at start of next day.
      return getEndDate().plusDays(1).atStartOfDay(zoneId).toInstant();
    }
  }

  @Data
  @SuperBuilder
  public abstract static class SLOTarget {
    public abstract TimePeriod getCurrentTimeRange(LocalDate currentDate);
    public abstract SLOTargetType getType();
  }
  @Data
  @SuperBuilder
  public abstract static class CalenderSLOTarget extends SLOTarget {
    private final SLOTargetType type = SLOTargetType.CALENDER;
    public abstract SLOCalenderType getCalenderType();
  }

  @Value
  @SuperBuilder
  public static class WeeklyCalenderTarget extends CalenderSLOTarget {
    private DayOfWeek dayOfWeek;
    private final SLOCalenderType calenderType = SLOCalenderType.WEEKLY;

    @Override
    public TimePeriod getCurrentTimeRange(LocalDate currentDate) {
      LocalDate nextDayOfWeek = dayOfWeek.getNextDayOfWeek(currentDate);
      return TimePeriod.builder().startDate(nextDayOfWeek.minusDays(6)).endDate(nextDayOfWeek).build();
    }
  }

  @SuperBuilder
  @Data
  public static class MonthlyCalenderTarget extends CalenderSLOTarget {
    int windowEndDayOfMonth;
    private final SLOCalenderType calenderType = SLOCalenderType.MONTHLY;

    @Override
    public TimePeriod getCurrentTimeRange(LocalDate currentDate) {
      LocalDate windowStart = getWindowEnd(currentDate.minusMonths(1), windowEndDayOfMonth).plusDays(1);
      LocalDate windowEnd = getWindowEnd(currentDate, windowEndDayOfMonth);
      return TimePeriod.builder().startDate(windowStart).endDate(windowEnd).build();
    }
    private LocalDate getWindowEnd(LocalDate currentDate, int windowEndDayOfMonth) {
      LocalDate windowEnd;
      if (windowEndDayOfMonth > 28) {
        windowEnd = currentDate.with(TemporalAdjusters.lastDayOfMonth());
      } else if (currentDate.getDayOfMonth() <= windowEndDayOfMonth) {
        windowEnd = getWindowEnd(currentDate);
      } else {
        windowEnd = getWindowEnd(currentDate.plusMonths(1));
      }
      return windowEnd;
    }

    private LocalDate getWindowEnd(LocalDate date) {
      return date.plusDays(windowEndDayOfMonth - date.getDayOfMonth());
    }
  }

  @SuperBuilder
  @Data
  public static class QuarterlyCalenderTarget extends CalenderSLOTarget {
    private final SLOCalenderType calenderType = SLOCalenderType.QUARTERLY;

    @Override
    public TimePeriod getCurrentTimeRange(LocalDate currentDate) {
      LocalDate firstDayOfQuarter =
          currentDate.with(currentDate.getMonth().firstMonthOfQuarter()).with(TemporalAdjusters.firstDayOfMonth());

      LocalDate lastDayOfQuarter = firstDayOfQuarter.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
      return TimePeriod.builder().startDate(firstDayOfQuarter).endDate(lastDayOfQuarter).build();
    }
  }

  @SuperBuilder
  @Data
  public static class RollingSLOTarget extends SLOTarget {
    int periodLengthDays;
    private final SLOTargetType type = SLOTargetType.ROLLING;

    @Override
    public TimePeriod getCurrentTimeRange(LocalDate currentDate) {
      // TODO: change to minute based.
      return TimePeriod.builder().endDate(currentDate).startDate(currentDate.minusDays(periodLengthDays)).build();
    }
  }
}
