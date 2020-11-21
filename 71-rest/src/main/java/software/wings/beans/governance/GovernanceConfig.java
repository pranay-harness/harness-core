package software.wings.beans.governance;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.CollectionUtils;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.governance.WeeklyFreezeConfig;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
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
public class GovernanceConfig implements PersistentEntity, UuidAware, UpdatedByAware, AccountAccess {
  @Id private String uuid;

  @FdIndex private String accountId;
  private boolean deploymentFreeze;
  private EmbeddedUser lastUpdatedBy;
  private List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs;
  private List<WeeklyFreezeConfig> weeklyFreezeConfigs;

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
}
