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
