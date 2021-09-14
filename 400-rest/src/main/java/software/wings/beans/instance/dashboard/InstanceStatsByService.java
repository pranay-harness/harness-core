/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/13/17
 */
@Data
@Builder
public class InstanceStatsByService {
  private long totalCount;
  private ServiceSummary serviceSummary;
  private List<InstanceStatsByEnvironment> instanceStatsByEnvList;

  public InstanceStatsByService clone(long newCount) {
    return InstanceStatsByService.builder()
        .totalCount(newCount)
        .serviceSummary(serviceSummary)
        .instanceStatsByEnvList(instanceStatsByEnvList)
        .build();
  }
}
