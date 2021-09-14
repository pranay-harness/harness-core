/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import lombok.Data;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
public class LogMLAnalysisRequest {
  private final String query;
  private final String applicationId;
  private final String stateExecutionId;
  private final Integer logCollectionMinute;
}
