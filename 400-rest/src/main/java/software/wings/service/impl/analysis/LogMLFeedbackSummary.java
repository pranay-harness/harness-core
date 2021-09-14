/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import io.harness.beans.EmbeddedUser;

import lombok.Builder;
import lombok.Data;

/**
 * @author Praveen
 */
@Data
@Builder
public class LogMLFeedbackSummary {
  private FeedbackPriority priority;
  private String logMLFeedbackId;
  private String jiraLink;
  private String feedbackNote;
  private EmbeddedUser lastUpdatedBy;
  private long lastUpdatedAt;
}
