package io.harness.ng.activitytracker.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CountPerActivityType {
  ActivityType activityType;
  long count;
}
