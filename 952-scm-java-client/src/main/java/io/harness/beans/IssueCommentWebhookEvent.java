package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.beans.WebhookEvent.Type.ISSUE_COMMENT;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("IssueComment")
@OwnedBy(DX)
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
