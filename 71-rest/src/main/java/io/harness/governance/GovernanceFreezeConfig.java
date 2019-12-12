package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.data.structure.CollectionUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonCreator;
import software.wings.beans.Environment.EnvironmentType;

import java.util.List;

@ToString
@EqualsAndHashCode
public abstract class GovernanceFreezeConfig {
  private boolean freezeForAllApps;
  private List<String> appIds;
  private List<EnvironmentType> environmentTypes;

  @JsonCreator
  public GovernanceFreezeConfig(@JsonProperty("freezeForAllApps") boolean freezeForAllApps,
      @JsonProperty("appIds") List<String> appIds,
      @JsonProperty("environmentTypes") List<EnvironmentType> environmentTypes) {
    this.freezeForAllApps = freezeForAllApps;
    this.appIds = appIds;
    this.environmentTypes = environmentTypes;
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
}
