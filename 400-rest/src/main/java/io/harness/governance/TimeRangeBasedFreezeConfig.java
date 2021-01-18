package io.harness.governance;

import io.harness.beans.EnvironmentType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.resources.stats.model.TimeRange;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonCreator;

@Getter
@ToString
@EqualsAndHashCode
@ParametersAreNonnullByDefault
public class TimeRangeBasedFreezeConfig extends GovernanceFreezeConfig {
  // if freezeForAllApps=true, ignore appIds
  private TimeRange timeRange;

  public TimeRange getTimeRange() {
    return timeRange;
  }

  @JsonCreator
  public TimeRangeBasedFreezeConfig(@JsonProperty("freezeForAllApps") boolean freezeForAllApps,
      @JsonProperty("appIds") List<String> appIds,
      @JsonProperty("environmentTypes") List<EnvironmentType> environmentTypes,
      @JsonProperty("timeRange") TimeRange timeRange, @JsonProperty("name") String name,
      @JsonProperty("description") String description, @JsonProperty("applicable") boolean applicable,
      @JsonProperty("appSelections") List<ApplicationFilter> appSelections,
      @JsonProperty("userGroups") List<String> userGroups) {
    super(freezeForAllApps, appIds, environmentTypes, name, description, applicable, appSelections, userGroups);
    this.timeRange = Objects.requireNonNull(timeRange, "time-range not provided for deployment freeze");

    if (timeRange.getFrom() > timeRange.getTo()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Start time of time-range must be strictly less than end time.");
    }
  }
}
