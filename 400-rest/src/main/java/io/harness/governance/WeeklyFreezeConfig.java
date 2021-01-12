package io.harness.governance;

import software.wings.beans.EnvironmentType;
import software.wings.resources.stats.model.WeeklyRange;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonCreator;

@Getter
@ToString
@EqualsAndHashCode
@ParametersAreNonnullByDefault
public class WeeklyFreezeConfig extends GovernanceFreezeConfig {
  // if freezeForAllApps=true, ignore appIds
  private WeeklyRange weeklyRange;

  public WeeklyRange getWeeklyRange() {
    return weeklyRange;
  }

  @JsonCreator
  public WeeklyFreezeConfig(@JsonProperty("freezeForAllApps") boolean freezeForAllApps,
      @JsonProperty("appIds") List<String> appIds,
      @JsonProperty("environmentTypes") List<EnvironmentType> environmentTypes,
      @JsonProperty("weeklyRange") WeeklyRange weeklyRange) {
    super(freezeForAllApps, appIds, environmentTypes);
    this.weeklyRange = weeklyRange;
  }
}
