/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
