package software.wings.beans.governance;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.CollectionUtils;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author rktummala on 02/04/19
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "governanceConfig", noClassnameStored = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "GovernanceConfigKeys")
public class GovernanceConfig implements PersistentEntity, UuidAware, UpdatedByAware {
  @Id private String uuid;

  @Indexed private String accountId;
  private boolean deploymentFreeze;
  private EmbeddedUser lastUpdatedBy;
  private List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs;

  @Builder
  public GovernanceConfig(
      String accountId, boolean deploymentFreeze, List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs) {
    this.accountId = accountId;
    this.deploymentFreeze = deploymentFreeze;
    this.timeRangeBasedFreezeConfigs = timeRangeBasedFreezeConfigs;
  }

  @Nonnull
  public List<TimeRangeBasedFreezeConfig> getTimeRangeBasedFreezeConfigs() {
    return CollectionUtils.emptyIfNull(timeRangeBasedFreezeConfigs);
  }
}
