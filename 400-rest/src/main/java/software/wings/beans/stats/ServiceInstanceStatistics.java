/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.stats;

import io.harness.beans.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceInstanceStatistics extends WingsStatistics {
  private Map<EnvironmentType, List<TopConsumer>> statsMap = new HashMap<>();

  public ServiceInstanceStatistics() {
    super(StatisticsType.SERVICE_INSTANCE_STATISTICS);
  }
}
