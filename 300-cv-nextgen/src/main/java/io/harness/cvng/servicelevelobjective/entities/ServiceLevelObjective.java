package io.harness.cvng.servicelevelobjective.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.servicelevelobjective.beans.SLOTarget;
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

  public int getTotalErrorBudgetMinutes(LocalDate currentDate) {
    int currentWindowMinutes = getCurrentTimeRange(currentDate).totalMinutes();
    Double errorBudgetPercentage = sloTarget.getSloTargetPercentage();
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
}
