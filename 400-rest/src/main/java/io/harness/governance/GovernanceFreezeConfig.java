package io.harness.governance;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.beans.EnvironmentType;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonCreator;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@ToString
@EqualsAndHashCode
public abstract class GovernanceFreezeConfig implements UuidAware {
  // not used
  @EqualsAndHashCode.Exclude private boolean freezeForAllApps;
  @EqualsAndHashCode.Exclude private List<String> appIds;
  @EqualsAndHashCode.Exclude private List<EnvironmentType> environmentTypes;

  private String uuid;
  @NotEmpty private String name;
  private String description;
  @EqualsAndHashCode.Exclude private boolean applicable; // Corresponds to the on/off button for each window
  private List<ApplicationFilter> appSelections;
  private List<String> userGroups; // User groups to be notified

  @JsonCreator
  public GovernanceFreezeConfig(@JsonProperty("freezeForAllApps") boolean freezeForAllApps,
      @JsonProperty("appIds") List<String> appIds,
      @JsonProperty("environmentTypes") List<EnvironmentType> environmentTypes, @JsonProperty("name") String name,
      @JsonProperty("description") String description, @JsonProperty("applicable") boolean applicable,
      @JsonProperty("appSelections") List<ApplicationFilter> appSelections,
      @JsonProperty("userGroups") List<String> userGroups, @JsonProperty("uuid") String uuid) {
    this.freezeForAllApps = freezeForAllApps;
    this.appIds = appIds;
    this.environmentTypes = environmentTypes;
    this.name = name;
    this.description = description;
    this.applicable = applicable;
    this.appSelections = appSelections;
    this.uuid = EmptyPredicate.isEmpty(uuid) ? generateUuid() : uuid;
    this.userGroups = userGroups;
  }

  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  public List<EnvironmentType> getEnvironmentTypes() {
    return CollectionUtils.emptyIfNull(environmentTypes);
  }
  public boolean isFreezeForAllApps() {
    return freezeForAllApps;
  }

  public abstract long fetchEndTime();

  // this method takes into account applicable boolean as well
  public boolean equalsWithApplicable(final Object o) {
    if (!this.equals(o)) {
      return false;
    }
    return ((GovernanceFreezeConfig) o).applicable == this.isApplicable();
  }
}
