/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.metrics.RiskLevel;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class LogMLClusterSummary {
  private Map<String, LogMLHostSummary> hostSummary;
  private String logText;
  private List<String> tags;
  private double score;
  private RiskLevel riskLevel;
  private FeedbackPriority priority;
  private int clusterLabel;
  private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;
  private String logMLFeedbackId;
  private String jiraLink;
  private LogMLFeedbackSummary feedbackSummary;
}
