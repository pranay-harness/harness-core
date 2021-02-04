package io.harness.ngtriggers.beans.scm;

import static io.harness.ngtriggers.beans.scm.WebhookEvent.Type.ISSUE_COMMENT;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("IssueComment")
public class IssueCommentWebhookEvent implements WebhookEvent {
  private String pullRequestNum;
  private String commentBody;
  private Repository repository;
  private WebhookBaseAttributes baseAttributes;

  @Override
  public Type getType() {
    return ISSUE_COMMENT;
  }
}
