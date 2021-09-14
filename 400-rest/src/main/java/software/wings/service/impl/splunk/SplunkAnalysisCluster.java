/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.splunk;

import software.wings.service.impl.analysis.FeedbackPriority;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 6/28/17.
 */
@Data
public class SplunkAnalysisCluster {
  private List<MessageFrequency> message_frequencies;
  private int cluster_label;
  private List<String> tags = new ArrayList<>();
  private List<Integer> anomalous_counts = new ArrayList<>();
  private boolean unexpected_freq;
  private String text;
  private double x;
  private double y;
  private String feedback_id;
  private FeedbackPriority priority;

  private List<String> diff_tags = new ArrayList<>();

  private double alert_score;
  private double test_score;
  private double control_score;
  private double freq_score;
  private int control_label;
  private double risk_level = 1.0;

  @Data
  @Builder
  public static class MessageFrequency {
    private int count;
    @JsonProperty("old_label") private String oldLabel;
    private String host;
    private long time;
  }
}
