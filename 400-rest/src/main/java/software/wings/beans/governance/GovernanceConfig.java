package software.wings.beans.governance;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.governance.WeeklyFreezeConfig;
import io.harness.iterator.PersistentCronIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * @author rktummala on 02/04/19
 */
@JsonInclude(NON_EMPTY)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "GovernanceConfigKeys")
@Entity(value = "governanceConfig", noClassnameStored = true)
@HarnessEntity(exportable = true)
@Slf4j
@TargetModule(Module._980_COMMONS)
public class GovernanceConfig
    implements PersistentEntity, UuidAware, UpdatedByAware, AccountAccess, PersistentCronIterable {
  @Id private String uuid;

  @FdIndex private String accountId;
  private boolean deploymentFreeze;
  private EmbeddedUser lastUpdatedBy;
  private List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs;
  private List<WeeklyFreezeConfig> weeklyFreezeConfigs;
  @FdIndex
  private List<Long> nextIterations; // List of activation times for all freeze windows used by activation handler
  @FdIndex
  private List<Long>
      nextCloseIterations; // List of deactivation time for all freeze windows used by deactivation handler

  @Builder
  public GovernanceConfig(String accountId, boolean deploymentFreeze,
      List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs, List<WeeklyFreezeConfig> weeklyFreezeConfigs) {
    this.accountId = accountId;
    this.deploymentFreeze = deploymentFreeze;
    this.timeRangeBasedFreezeConfigs = timeRangeBasedFreezeConfigs;
    this.weeklyFreezeConfigs = weeklyFreezeConfigs;
  }

  @Nonnull
  public List<TimeRangeBasedFreezeConfig> getTimeRangeBasedFreezeConfigs() {
    return CollectionUtils.emptyIfNull(timeRangeBasedFreezeConfigs);
  }

  @Nonnull
  public List<WeeklyFreezeConfig> getWeeklyFreezeConfigs() {
    return CollectionUtils.emptyIfNull(weeklyFreezeConfigs);
  }

  @Override
  public List<Long> recalculateNextIterations(String fieldName, boolean skipMissing, long throttled) {
    if (EmptyPredicate.isEmpty(timeRangeBasedFreezeConfigs)) {
      nextIterations = new ArrayList();
      nextCloseIterations = new ArrayList();
      return new ArrayList<>();
    }
    try {
      long currentTime = System.currentTimeMillis();
      if (GovernanceConfigKeys.nextIterations.equals(fieldName)) {
        nextIterations = timeRangeBasedFreezeConfigs.stream()
                             .filter(TimeRangeBasedFreezeConfig::isApplicable)
                             .map(freeze -> freeze.getTimeRange().getFrom())
                             .distinct()
                             .sorted()
                             .filter(time -> time > currentTime)
                             .collect(Collectors.toList());
        return nextIterations;
      } else {
        nextCloseIterations = timeRangeBasedFreezeConfigs.stream()
                                  .filter(TimeRangeBasedFreezeConfig::isApplicable)
                                  .map(freeze -> freeze.getTimeRange().getTo())
                                  .distinct()
                                  .sorted()
                                  .filter(time -> time > currentTime)
                                  .collect(Collectors.toList());
        return nextCloseIterations;
      }
    } catch (Exception ex) {
      log.error("Failed to schedule notification for governance config {}", uuid, ex);
      throw ex;
    }
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (GovernanceConfigKeys.nextIterations.equals(fieldName)) {
      return EmptyPredicate.isEmpty(nextIterations) ? null : nextIterations.get(0);
    }
    return EmptyPredicate.isEmpty(nextCloseIterations) ? null : nextCloseIterations.get(0);
  }
}
