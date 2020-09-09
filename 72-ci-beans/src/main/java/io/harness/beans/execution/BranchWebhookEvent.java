package io.harness.beans.execution;

import static io.harness.beans.execution.WebhookEvent.Type.BRANCH;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonTypeName("Branch")
public class BranchWebhookEvent implements WebhookEvent {
  private String branchName;
  private String link;
  private List<CommitDetails> commitDetailsList;

  @Override
  public Type getType() {
    return BRANCH;
  }
}
