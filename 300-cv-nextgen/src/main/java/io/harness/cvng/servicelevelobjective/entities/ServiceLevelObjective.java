package io.harness.cvng.servicelevelobjective.entities;

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
import java.time.LocalDate;
import java.time.Period;
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

  public int getTotalErrorBudgetMinutes(LocalDate currentDate) {
    int currentWindowMinutes = getCurrentTimeRange(currentDate).totalMinutes();
    Double errorBudgetPercentage = getSloTargetPercentage();
    return (int) ((100 - errorBudgetPercentage) * currentWindowMinutes);
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
    return TimePeriod.builder()
        .startDate(currentDate)
        .endDate(currentDate.minus(Period.ofDays(7)))
        .build(); // TODO: write this logic.
  }

  @Value
  @Builder
  public static class TimePeriod {
    LocalDate startDate;
    LocalDate endDate;

    public Period getRemainingDays(LocalDate currentDate) {
      return Period.between(currentDate, endDate);
    }
    public Period getTotalDays() {
      return Period.between(startDate, endDate);
    }
    public int totalMinutes() {
      return Period.between(startDate, endDate).getDays() * 24 * 60;
    }
  }

  @Data
  @SuperBuilder
  public abstract static class SLOTarget {
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
  }

  @SuperBuilder
  @Data
  public static class MonthlyCalenderTarget extends CalenderSLOTarget {
    int dayOfMonth;
    private final SLOCalenderType calenderType = SLOCalenderType.MONTHLY;
  }

  @SuperBuilder
  @Data
  public static class QuarterlyCalenderTarget extends CalenderSLOTarget {
    private final SLOCalenderType calenderType = SLOCalenderType.QUARTERLY;
  }

  @SuperBuilder
  @Data
  public static class RollingSLOTarget extends SLOTarget {
    int periodLengthDays;
    private final SLOTargetType type = SLOTargetType.ROLLING;
  }
}
