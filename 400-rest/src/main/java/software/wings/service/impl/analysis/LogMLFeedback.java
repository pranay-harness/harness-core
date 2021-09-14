/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogMLFeedback {
  private String appId;
  private String stateExecutionId;
  private AnalysisServiceImpl.CLUSTER_TYPE clusterType;
  private int clusterLabel;
  private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;
  private String comment;
  private String logMLFeedbackId;
  private long analysisMinute;
  private String serviceId;
  private String envId;
}
