/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 6/20/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogElement {
  private String query;
  private String clusterLabel;
  private String host;
  private long timeStamp;
  private int count;
  private String logMessage;
  private long logCollectionMinute;
}
