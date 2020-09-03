package io.harness.beans.execution;

import static io.harness.beans.execution.WebhookEvent.Type.PR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonTypeName("PR")
public class PRWebhookEvent implements WebhookEvent {
  private Long pullRequestId;
  private String pullRequestLink;
  private String pullRequestBody;
  private String sourceBranch;
  private String targetBranch;
  private String title;
  private boolean closed;
  private boolean merged;
  private List<CommitDetails> commitDetailsList;

  @Override
  public Type getType() {
    return PR;
  }
}
