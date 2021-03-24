package io.harness.governance;

import io.harness.beans.EnvironmentType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.resources.stats.model.TimeRange;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.annotate.JsonCreator;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@Slf4j
@ParametersAreNonnullByDefault
public class TimeRangeBasedFreezeConfig extends GovernanceFreezeConfig {
  // if freezeForAllApps=true, ignore appIds
  @Setter private TimeRange timeRange;

  public TimeRange getTimeRange() {
    return timeRange;
  }

  @Builder
  @JsonCreator
  public TimeRangeBasedFreezeConfig(@JsonProperty("freezeForAllApps") boolean freezeForAllApps,
      @JsonProperty("appIds") List<String> appIds,
      @JsonProperty("environmentTypes") List<EnvironmentType> environmentTypes,
      @JsonProperty("timeRange") TimeRange timeRange, @JsonProperty("name") String name,
      @JsonProperty("description") String description, @JsonProperty("applicable") boolean applicable,
      @JsonProperty("appSelections") List<ApplicationFilter> appSelections,
      @JsonProperty("userGroups") List<String> userGroups, @JsonProperty("uuid") String uuid) {
    super(freezeForAllApps, appIds, environmentTypes, name, description, applicable, appSelections, userGroups, uuid);
    this.timeRange = Objects.requireNonNull(timeRange, "time-range not provided for deployment freeze");

    if (timeRange.getFrom() > timeRange.getTo()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Start time of time-range must be strictly less than end time.");
    }
  }

  public boolean checkIfActive() {
    if (!isApplicable()) {
      return false;
    }
    long currentTime = System.currentTimeMillis();
    log.info("Window id: {}, Current time: {}, from: {}, to: {}", getUuid(), currentTime, getTimeRange().getFrom(),
        getTimeRange().getTo());
    if (timeRange == null) {
      log.warn("Time range is null for deployment freeze window: " + getUuid());
      return false;
    }
    return currentTime <= getTimeRange().getTo() && currentTime >= getTimeRange().getFrom();
  }

  @Override
  public long fetchEndTime() {
    return getTimeRange().getTo();
  }
}
