/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import software.wings.beans.infrastructure.instance.InvocationCount;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 01/03/18
 */
@Data
@Builder
public class InstanceSummaryStatsByService {
  private long totalCount;
  private long prodCount;
  private long nonprodCount;
  private ServiceSummary serviceSummary;
  private InvocationCount invocationCount;
}
