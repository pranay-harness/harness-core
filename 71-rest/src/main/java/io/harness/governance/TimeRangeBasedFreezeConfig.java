package io.harness.governance;

import io.harness.data.structure.CollectionUtils;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonCreator;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.resources.stats.model.TimeRange;

import java.util.List;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@ParametersAreNonnullByDefault
public class TimeRangeBasedFreezeConfig {
  // if freezeForAllApps=true, ignore appIds
  private boolean freezeForAllApps;
  private List<String> appIds;

  private List<EnvironmentType> environmentTypes;
  private TimeRange timeRange;

  @JsonCreator
  public TimeRangeBasedFreezeConfig(
      boolean freezeForAllApps, List<String> appIds, List<EnvironmentType> environmentTypes, TimeRange timeRange) {
    this.freezeForAllApps = freezeForAllApps;
    this.appIds = appIds;
    this.environmentTypes = environmentTypes;
    this.timeRange = Objects.requireNonNull(timeRange, "time-range not provided for deployment freeze");

    if (timeRange.getFrom() > timeRange.getTo()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Start time of time-range must be strictly less than end time.");
    }
  }

  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  public List<EnvironmentType> getEnvironmentTypes() {
    return CollectionUtils.emptyIfNull(environmentTypes);
  }
}
