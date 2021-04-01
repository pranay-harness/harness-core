package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.beans.WebhookEvent.Type.BRANCH;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Branch")
@OwnedBy(DX)
public class PushWebhookEvent implements WebhookEvent {
  private String branchName;
  private String link;
  private List<CommitDetails> commitDetailsList;
  private Repository repository;
  private WebhookBaseAttributes baseAttributes;

  @Override
  public Type getType() {
    return BRANCH;
  }
}
